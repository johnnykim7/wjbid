package com.biddingagency.domain.notification.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_logs",
        indexes = {
                @Index(name = "idx_notification_type", columnList = "notification_type"),
                @Index(name = "idx_notification_reference", columnList = "reference_type, reference_id"),
                @Index(name = "idx_notification_sent_at", columnList = "sent_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uniq_notification_idempotency", columnNames = "idempotency_key")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NotificationLog extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @Column(name = "recipient_id", columnDefinition = "BINARY(16)")
    private UUID recipientId;

    @Column(name = "subject", nullable = false, length = 500)
    private String subject;

    @Column(name = "reference_id", columnDefinition = "BINARY(16)")
    private UUID referenceId;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;
}
