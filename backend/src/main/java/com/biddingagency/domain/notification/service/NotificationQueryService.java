package com.biddingagency.domain.notification.service;

import com.biddingagency.domain.notification.dto.NotificationDto;
import com.biddingagency.domain.notification.entity.NotificationLog;
import com.biddingagency.domain.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 고객 인앱 알림 조회/읽음 처리 서비스 (CR-006).
 *
 * 본인(recipientId) 알림만 조회/처리 가능. 발송 성공분만 노출한다.
 */
@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationLogRepository notificationLogRepository;

    @Transactional(readOnly = true)
    public Page<NotificationDto> getMyNotifications(UUID memberId, Pageable pageable) {
        return notificationLogRepository
                .findByRecipientIdAndSuccessTrueOrderBySentAtDesc(memberId, pageable)
                .map(NotificationDto::from);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID memberId) {
        return notificationLogRepository.countByRecipientIdAndSuccessTrueAndReadAtIsNull(memberId);
    }

    /**
     * 단건 읽음 처리. 본인 알림이 아니면 무시(존재하지 않는 것으로 취급).
     */
    @Transactional
    public void markAsRead(UUID notificationId, UUID memberId) {
        notificationLogRepository.findByIdAndRecipientId(notificationId, memberId)
                .ifPresent(NotificationLog::markAsRead);
    }
}
