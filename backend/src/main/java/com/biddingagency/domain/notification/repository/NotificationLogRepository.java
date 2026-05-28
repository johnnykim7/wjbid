package com.biddingagency.domain.notification.repository;

import com.biddingagency.domain.notification.entity.NotificationLog;
import com.biddingagency.domain.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<NotificationLog> findByNotificationType(NotificationType type);

    List<NotificationLog> findByReferenceTypeAndReferenceId(String referenceType, UUID referenceId);

    // CR-006: 고객 인앱 알림 조회 — 발송 성공분만 노출, 최신순
    Page<NotificationLog> findByRecipientIdAndSuccessTrueOrderBySentAtDesc(UUID recipientId, Pageable pageable);

    long countByRecipientIdAndSuccessTrueAndReadAtIsNull(UUID recipientId);

    Optional<NotificationLog> findByIdAndRecipientId(UUID id, UUID recipientId);
}
