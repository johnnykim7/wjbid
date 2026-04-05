package com.biddingagency.domain.notification.repository;

import com.biddingagency.domain.notification.entity.NotificationLog;
import com.biddingagency.domain.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<NotificationLog> findByNotificationType(NotificationType type);

    List<NotificationLog> findByReferenceTypeAndReferenceId(String referenceType, UUID referenceId);
}
