package com.biddingagency.domain.notification.service;

import com.biddingagency.domain.event.DocumentExpiryApproachingEvent;
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
 * 서류 만료 메일 (CR-136, BID-MKT-014). 템플릿 BID_DOC_EXPIRY.
 *
 * 멱등 키에 만료일과 시점을 넣어, 만료일을 바꾸면 새 일정으로 다시 보낸다(BIZ-030).
 * 보내기에 실패한 시점은 다시 보내지 않고 알림 이력에 실패로 남는다(BIZ-012).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentExpiryNotificationListener {

    /** 알림 이력 제목. 메일 제목은 템플릿이 정한다 */
    private static final String LOG_SUBJECT = "서류 유효기간 만료 %d일 전: %s";
    private static final String EXPIRY_PATH = "/documents/expiry";
    static final String REFERENCE_TYPE = "MemberDocument";

    private final NotificationService notificationService;
    private final MemberRepository memberRepository;

    @Value("${app.customer-portal-url:http://localhost:3183}")
    private String customerPortalUrl;

    @Async
    @EventListener
    public void onDocumentExpiryApproaching(DocumentExpiryApproachingEvent event) {
        Member member = memberRepository.findById(event.getMemberId()).orElse(null);
        if (member == null || member.getEmail() == null || member.getEmail().isBlank()) {
            log.warn("서류 만료 메일 받을 회원이 없습니다: doc={}, member={}", event.getEntityId(), event.getMemberId());
            return;
        }
        Map<String, Object> vars = new HashMap<>();
        vars.put("customerName", displayName(member));
        vars.put("documentName", event.getDisplayName());
        vars.put("documentKind", event.getCategoryLabel());
        vars.put("expiresOn", event.getExpiresOn().toString());
        vars.put("daysRemaining", String.valueOf(event.getDaysRemaining()));
        vars.put("expiryUrl", customerPortalUrl + EXPIRY_PATH);
        try {
            notificationService.sendNotification(NotificationType.DOCUMENT_EXPIRY,
                    member.getEmail(), member.getId(),
                    String.format(LOG_SUBJECT, event.getDaysRemaining(), event.getDisplayName()),
                    MailHtmlValues.escape(vars), event.getEntityId(), REFERENCE_TYPE,
                    "DOC_EXPIRY_" + event.getEntityId() + "_" + event.getExpiresOn() + "_" + event.getDaysRemaining());
        } catch (Exception e) {
            log.error("서류 만료 메일 실패: doc={}", event.getEntityId(), e);
        }
    }

    /** 회원 표시명: 담당자명 우선, 없으면 회사명, 그것도 없으면 이메일 */
    private static String displayName(Member m) {
        if (m.getContactPerson() != null && !m.getContactPerson().isBlank()) return m.getContactPerson();
        if (m.getCompanyName() != null && !m.getCompanyName().isBlank()) return m.getCompanyName();
        return m.getEmail();
    }
}
