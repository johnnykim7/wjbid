package com.biddingagency.domain.notification.dto;

import com.biddingagency.domain.notification.entity.NotificationLog;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 고객 인앱 알림 응답 DTO (CR-006).
 */
public record NotificationDto(
        UUID id,
        String type,
        String subject,
        UUID referenceId,
        String referenceType,
        LocalDateTime sentAt,
        boolean read,
        LocalDateTime readAt) {

    public static NotificationDto from(NotificationLog log) {
        return new NotificationDto(
                log.getId(),
                log.getNotificationType().name(),
                log.getSubject(),
                log.getReferenceId(),
                log.getReferenceType(),
                log.getSentAt(),
                log.getReadAt() != null,
                log.getReadAt());
    }
}
