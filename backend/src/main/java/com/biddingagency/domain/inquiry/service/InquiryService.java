package com.biddingagency.domain.inquiry.service;

import com.biddingagency.domain.event.InquiryReceivedEvent;
import com.biddingagency.domain.inquiry.entity.Inquiry;
import com.biddingagency.domain.inquiry.entity.InquiryStatus;
import com.biddingagency.domain.inquiry.entity.InquiryType;
import com.biddingagency.domain.inquiry.exception.InquiryAlreadyAnsweredException;
import com.biddingagency.domain.inquiry.exception.InquiryAnswerMailFailedException;
import com.biddingagency.domain.inquiry.exception.InquiryNotFoundException;
import com.biddingagency.domain.inquiry.exception.InquiryTypeInvalidException;
import com.biddingagency.domain.inquiry.repository.InquiryRepository;
import com.biddingagency.domain.inquiry.repository.InquiryTypeRepository;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.domain.notification.entity.NotificationLog;
import com.biddingagency.domain.notification.entity.NotificationType;
import com.biddingagency.domain.notification.repository.NotificationLogRepository;
import com.biddingagency.domain.notification.service.MailHtmlValues;
import com.biddingagency.domain.notification.service.NotificationService;
import com.biddingagency.dto.InquiryResponses;
import com.biddingagency.dto.InquirySubmitRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 고객센터 1:1 문의 (CR-132, BID-SUPPORT-001, 003).
 */
@Service
@RequiredArgsConstructor
public class InquiryService {

    static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final InquiryRepository inquiryRepository;
    private final InquiryTypeRepository inquiryTypeRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final NotificationLogRepository notificationLogRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<InquiryResponses.TypeItem> listTypes() {
        return inquiryTypeRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(t -> new InquiryResponses.TypeItem(t.getCode(), t.getLabel()))
                .toList();
    }

    /**
     * 문의를 접수한다. 로그인 고객이면 회원과 묶고, 이름과 이메일은 고객이 적은 값을 그대로 둔다.
     * 관리자 알림은 커밋 뒤 따로 나가므로 알림이 실패해도 접수는 남는다(BIZ-023).
     */
    @Transactional
    public InquiryResponses.Submitted submit(InquirySubmitRequest request, Member member) {
        InquiryType type = inquiryTypeRepository.findById(request.getTypeCode())
                .filter(t -> Boolean.TRUE.equals(t.getActive()))
                .orElseThrow(InquiryTypeInvalidException::new);
        UUID memberId = member != null ? member.getId() : null;

        Inquiry saved = inquiryRepository.save(Inquiry.builder()
                .typeCode(type.getCode())
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .name(request.getName().trim())
                .email(request.getEmail().trim())
                .memberId(memberId)
                .status(InquiryStatus.RECEIVED)
                .build());

        eventPublisher.publishEvent(new InquiryReceivedEvent(saved.getId(), type.getLabel(), saved.getTitle(),
                saved.getContent(), saved.getName(), saved.getEmail(), memberId, saved.getCreatedAt()));
        return new InquiryResponses.Submitted(saved.getId().toString(), saved.getStatus().name(),
                format(saved.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public Page<InquiryResponses.ListItem> search(InquiryStatus status, String typeCode, int page, int size) {
        Map<String, String> labels = typeLabels();
        String code = typeCode == null || typeCode.isBlank() ? null : typeCode;
        return inquiryRepository.search(status, code,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(i -> new InquiryResponses.ListItem(i.getId().toString(), format(i.getCreatedAt()),
                        i.getTypeCode(), labels.getOrDefault(i.getTypeCode(), i.getTypeCode()), i.getTitle(),
                        i.getName(), i.getEmail(), i.getMemberId() != null, i.getStatus().name()));
    }

    @Transactional(readOnly = true)
    public InquiryResponses.Counts counts() {
        long received = inquiryRepository.countByStatus(InquiryStatus.RECEIVED);
        long answered = inquiryRepository.countByStatus(InquiryStatus.ANSWERED);
        return new InquiryResponses.Counts(received + answered, received, answered);
    }

    @Transactional(readOnly = true)
    public InquiryResponses.Detail detail(UUID id) {
        Inquiry i = inquiryRepository.findById(id).orElseThrow(InquiryNotFoundException::new);
        String typeLabel = inquiryTypeRepository.findById(i.getTypeCode())
                .map(InquiryType::getLabel).orElse(i.getTypeCode());
        String companyName = i.getMemberId() == null ? null
                : memberRepository.findById(i.getMemberId()).map(Member::getCompanyName).orElse(null);
        String answeredByEmail = i.getAnsweredBy() == null ? null
                : memberRepository.findById(i.getAnsweredBy()).map(Member::getEmail).orElse(null);

        return new InquiryResponses.Detail(i.getId().toString(), i.getTypeCode(), typeLabel, i.getTitle(),
                i.getContent(), i.getName(), i.getEmail(),
                i.getMemberId() != null ? i.getMemberId().toString() : null, companyName,
                i.getStatus().name(), format(i.getCreatedAt()), adminNotice(i.getId()),
                i.getAnswer(), format(i.getAnsweredAt()), answeredByEmail);
    }

    /**
     * 관리자 답변을 고객 메일로 보내고, 메일이 성공했을 때만 답변완료로 기록한다(BIZ-024).
     *
     * 메일과 기록을 한 트랜잭션으로 묶지 않는다. 메일이 실패하면 아무것도 바뀌지 않아야 다시 보낼 수 있고,
     * 메일 발송 이력은 실패해도 남아야 하기 때문이다. 발송 이력의 멱등 키는 유일해야 하므로
     * 시도마다 시각을 붙여 다시 보내기가 앞선 실패 이력에 막히지 않게 한다.
     */
    public InquiryResponses.Detail answer(UUID id, String answer, Member admin) {
        Inquiry i = inquiryRepository.findById(id).orElseThrow(InquiryNotFoundException::new);
        if (i.getStatus() == InquiryStatus.ANSWERED) {
            throw new InquiryAlreadyAnsweredException();
        }
        String body = answer.trim();
        String typeLabel = inquiryTypeRepository.findById(i.getTypeCode())
                .map(InquiryType::getLabel).orElse(i.getTypeCode());

        Map<String, Object> vars = new HashMap<>();
        vars.put("customerName", i.getName());
        vars.put("inquiryType", typeLabel);
        vars.put("inquiryTitle", i.getTitle());
        vars.put("inquiryContent", i.getContent());
        vars.put("receivedAt", format(i.getCreatedAt()));
        vars.put("answer", body);

        boolean sent = notificationService.sendNotification(NotificationType.INQUIRY_ANSWERED,
                i.getEmail(), i.getMemberId(), i.getTitle(), MailHtmlValues.escape(vars), i.getId(), Inquiry.REFERENCE_TYPE,
                "INQUIRY_ANSWERED_" + i.getId() + "_" + clock.millis());
        if (!sent) {
            throw new InquiryAnswerMailFailedException();
        }

        int updated = inquiryRepository.markAnswered(i.getId(), body, LocalDateTime.now(clock), admin.getId());
        if (updated == 0) {
            throw new InquiryAlreadyAnsweredException();
        }
        return detail(i.getId());
    }

    private InquiryResponses.AdminNotice adminNotice(UUID inquiryId) {
        List<NotificationLog> logs = notificationLogRepository
                .findByReferenceTypeAndReferenceId(Inquiry.REFERENCE_TYPE, inquiryId).stream()
                .filter(l -> l.getNotificationType() == NotificationType.INQUIRY_RECEIVED)
                .toList();
        long success = logs.stream().filter(l -> Boolean.TRUE.equals(l.getSuccess())).count();
        LocalDateTime last = logs.stream().map(NotificationLog::getSentAt).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);
        return new InquiryResponses.AdminNotice(logs.size(), success, format(last));
    }

    private Map<String, String> typeLabels() {
        return inquiryTypeRepository.findAll().stream()
                .collect(Collectors.toMap(InquiryType::getCode, InquiryType::getLabel));
    }

    static String format(LocalDateTime t) {
        return t != null ? t.format(FMT) : null;
    }
}
