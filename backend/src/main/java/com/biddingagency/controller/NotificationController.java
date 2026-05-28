package com.biddingagency.controller;

import com.biddingagency.domain.notification.dto.NotificationDto;
import com.biddingagency.domain.notification.service.NotificationQueryService;
import com.biddingagency.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * 고객 인앱 알림 API (CR-006).
 *
 * 본인 알림만 조회/읽음 처리한다. 인증 필수(SecurityConfig anyRequest authenticated).
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "고객 인앱 알림")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    @GetMapping
    @Operation(summary = "내 알림 목록", description = "발송 성공한 내 알림을 최신순으로 조회")
    public ResponseEntity<Page<NotificationDto>> getMyNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID memberId = userDetails.getMember().getId();
        return ResponseEntity.ok(
                notificationQueryService.getMyNotifications(memberId, pageable));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "미읽음 알림 수", description = "알림 벨 배지용 카운트")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID memberId = userDetails.getMember().getId();
        return ResponseEntity.ok(
                Map.of("unreadCount", notificationQueryService.getUnreadCount(memberId)));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "알림 읽음 처리", description = "단건 알림을 읽음으로 표시")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {
        UUID memberId = userDetails.getMember().getId();
        notificationQueryService.markAsRead(id, memberId);
        return ResponseEntity.ok(Map.of("id", id.toString(), "read", true));
    }
}
