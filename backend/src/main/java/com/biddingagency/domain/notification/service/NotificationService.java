package com.biddingagency.domain.notification.service;

import com.biddingagency.domain.notification.entity.NotificationLog;
import com.biddingagency.domain.notification.entity.NotificationType;
import com.biddingagency.domain.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final JavaMailSender mailSender;

    /**
     * Send notification with idempotency (BIZ-012)
     */
    @Transactional
    public void sendNotification(NotificationType type, String recipientEmail, UUID recipientId,
                                  String subject, String body, UUID referenceId,
                                  String referenceType, String idempotencyKey) {

        // BIZ-012: 멱등성 — 동일 키로 이미 발송했으면 무시
        if (notificationLogRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("Notification already sent (idempotency): key={}", idempotencyKey);
            return;
        }

        boolean success = false;
        String errorMessage = null;

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipientEmail);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("noreply@biddingagency.com");
            mailSender.send(message);
            success = true;
            log.info("Email sent: to={}, subject={}", recipientEmail, subject);
        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.error("Email send failed: to={}, error={}", recipientEmail, e.getMessage());
        }

        // 로그 저장
        NotificationLog logEntry = NotificationLog.builder()
                .notificationType(type)
                .recipientEmail(recipientEmail)
                .recipientId(recipientId)
                .subject(subject)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .sentAt(LocalDateTime.now())
                .success(success)
                .errorMessage(errorMessage)
                .idempotencyKey(idempotencyKey)
                .build();

        notificationLogRepository.save(logEntry);
    }
}
