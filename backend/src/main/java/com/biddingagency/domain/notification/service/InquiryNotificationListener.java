package com.biddingagency.domain.notification.service;

import com.biddingagency.domain.event.InquiryReceivedEvent;
import com.biddingagency.domain.inquiry.entity.Inquiry;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.domain.notification.entity.NotificationType;
import com.biddingagency.domain.setting.entity.SettingKey;
import com.biddingagency.domain.setting.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 1:1 문의 접수를 관리자에게 메일로 알린다 (CR-132, BID-SUPPORT-002).
 *
 * 접수가 커밋된 뒤에 비동기로 돌아 알림이 늦거나 실패해도 접수에는 영향이 없다(BIZ-023).
 * 받는 사람마다 멱등 키를 따로 둔다. 키를 함께 쓰면 첫 사람만 받고 나머지는 이미 보낸 것으로 건너뛴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InquiryNotificationListener {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final NotificationService notificationService;
    private final MemberRepository memberRepository;
    private final SystemSettingService systemSettingService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInquiryReceived(InquiryReceivedEvent event) {
        String companyName = event.getMemberId() == null ? ""
                : memberRepository.findById(event.getMemberId()).map(Member::getCompanyName).orElse("");

        Map<String, Object> vars = new HashMap<>();
        vars.put("inquiryType", event.getTypeLabel());
        vars.put("inquiryTitle", event.getTitle());
        vars.put("inquiryContent", event.getContent());
        vars.put("customerName", event.getName());
        vars.put("customerEmail", event.getEmail());
        vars.put("companyName", companyName);
        vars.put("receivedAt", event.getReceivedAt() != null ? event.getReceivedAt().format(FMT) : "");

        for (Map.Entry<String, UUID> to : recipients().entrySet()) {
            try {
                notificationService.sendNotification(NotificationType.INQUIRY_RECEIVED,
                        to.getKey(), to.getValue(), event.getTitle(), vars,
                        event.getEntityId(), Inquiry.REFERENCE_TYPE,
                        "INQUIRY_RECEIVED_" + event.getEntityId() + "_" + to.getKey());
            } catch (Exception e) {
                log.error("Inquiry admin notification failed: inquiryId={}, to={}", event.getEntityId(), to.getKey(), e);
            }
        }
    }

    /**
     * 받는 주소와 회원 id. 설정 주소가 하나도 쓸 만하지 않으면 관리자 전원에게 보낸다(POL-013).
     */
    Map<String, UUID> recipients() {
        Map<String, UUID> result = new LinkedHashMap<>();
        systemSettingService.findValue(SettingKey.INQUIRY_NOTIFY_EMAILS).ifPresent(value ->
                Arrays.stream(value.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .forEach(email -> {
                            if (EMAIL.matcher(email).matches()) {
                                result.putIfAbsent(email, memberRepository.findByEmail(email)
                                        .map(Member::getId).orElse(null));
                            } else {
                                log.warn("Skipping malformed {} entry: {}", SettingKey.INQUIRY_NOTIFY_EMAILS, email);
                            }
                        }));
        if (result.isEmpty()) {
            for (Member admin : memberRepository.findByRole(Member.Role.ADMIN)) {
                result.putIfAbsent(admin.getEmail(), admin.getId());
            }
        }
        return result;
    }
}
