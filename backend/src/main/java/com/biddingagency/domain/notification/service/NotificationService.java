package com.biddingagency.domain.notification.service;

import com.biddingagency.domain.notification.entity.NotificationLog;
import com.biddingagency.domain.notification.entity.NotificationType;
import com.biddingagency.domain.notification.repository.NotificationLogRepository;
import com.biddingagency.integration.notification.BpNotificationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 알림 발송 서비스 (CR-005/006).
 *
 * 발송은 bp-notification(외부 서비스)에 위임하고, 본 서비스는 멱등성(BIZ-012)과
 * NotificationLog 협력 호출 이력 저장을 책임진다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final BpNotificationClient bpNotificationClient;

    /**
     * 템플릿 기반 알림 발송 (멱등 처리 — BIZ-012).
     *
     * @param type          알림 유형 (→ bp-notification templateCode 매핑)
     * @param recipientEmail 수신자 이메일
     * @param recipientId   수신자 회원 ID (인앱 알림 조회용)
     * @param subject       알림 제목 (NotificationLog 기록용; 실제 메일 제목은 템플릿이 결정)
     * @param variables     템플릿 치환 변수
     * @param referenceId   참조 엔티티 ID
     * @param referenceType 참조 엔티티 타입
     * @param idempotencyKey 멱등 키
     */
    @Transactional
    public void sendNotification(NotificationType type, String recipientEmail, UUID recipientId,
                                 String subject, Map<String, Object> variables, UUID referenceId,
                                 String referenceType, String idempotencyKey) {

        // BIZ-012: 멱등성 — 동일 키로 이미 발송했으면 무시
        if (notificationLogRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("Notification already sent (idempotency): key={}", idempotencyKey);
            return;
        }

        BpNotificationClient.SendResult result = bpNotificationClient.sendEmail(
                recipientEmail, type.getTemplateCode(), variables);

        NotificationLog logEntry = NotificationLog.builder()
                .notificationType(type)
                .recipientEmail(recipientEmail)
                .recipientId(recipientId)
                .subject(subject)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .sentAt(LocalDateTime.now())
                .success(result.success())
                .errorMessage(result.errorMessage())
                .idempotencyKey(idempotencyKey)
                .build();

        notificationLogRepository.save(logEntry);
    }
}
