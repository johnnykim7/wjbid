package com.biddingagency.domain.notification.service;

import com.biddingagency.domain.event.ContractEndApproachingEvent;
import com.biddingagency.domain.federal.service.MemberContractService;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.domain.notification.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 계약 끝 메일 (CR-142, BID-MKT-033). 템플릿 BID_CONTRACT_END.
 *
 * 멱등 키에 날짜 종류, 날짜, 시점을 넣어, 날짜가 바뀌면 새 일정으로 다시 보낸다(BIZ-055).
 * 보내기에 실패한 시점은 다시 보내지 않고 알림 이력에 실패로 남는다(BIZ-012).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContractEndNotificationListener {

    /** 알림 이력 제목. 메일 제목은 템플릿이 정한다 */
    private static final String LOG_SUBJECT = "계약 %s %s %d일 전";
    private static final String ALERT_PATH = "/recompete/option-alert";

    private final NotificationService notificationService;
    private final MemberRepository memberRepository;

    @Value("${app.customer-portal-url:http://localhost:3183}")
    private String customerPortalUrl;

    @Async
    @EventListener
    public void onContractEndApproaching(ContractEndApproachingEvent event) {
        Member member = memberRepository.findById(event.getMemberId()).orElse(null);
        if (member == null || member.getEmail() == null || member.getEmail().isBlank()) {
            log.warn("계약 끝 메일 받을 회원이 없습니다: contract={}, member={}", event.getEntityId(), event.getMemberId());
            return;
        }
        Map<String, Object> vars = new HashMap<>();
        vars.put("customerName", displayName(member));
        vars.put("contractPiid", event.getPiid());
        vars.put("description", event.getDescription() == null ? "" : event.getDescription());
        vars.put("dateKindLabel", event.getDateKind().getLabel());
        vars.put("targetDate", event.getTargetDate().toString());
        vars.put("daysRemaining", String.valueOf(event.getDaysRemaining()));
        vars.put("alertUrl", customerPortalUrl + ALERT_PATH);
        try {
            notificationService.sendNotification(NotificationType.CONTRACT_END,
                    member.getEmail(), member.getId(),
                    String.format(LOG_SUBJECT, event.getPiid(), event.getDateKind().getLabel(), event.getDaysRemaining()),
                    MailHtmlValues.escape(vars), event.getEntityId(), MemberContractService.REFERENCE_TYPE,
                    MemberContractService.idempotencyKey(event.getEntityId(), event.getDateKind(),
                            event.getTargetDate(), event.getDaysRemaining()));
        } catch (Exception e) {
            log.error("계약 끝 메일 실패: contract={}", event.getEntityId(), e);
        }
    }

    /** 회원 표시명: 담당자명 우선, 없으면 회사명, 그것도 없으면 이메일 */
    private static String displayName(Member m) {
        if (m.getContactPerson() != null && !m.getContactPerson().isBlank()) return m.getContactPerson();
        if (m.getCompanyName() != null && !m.getCompanyName().isBlank()) return m.getCompanyName();
        return m.getEmail();
    }
}
