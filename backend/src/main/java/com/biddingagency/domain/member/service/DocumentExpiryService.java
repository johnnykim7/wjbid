package com.biddingagency.domain.member.service;

import com.biddingagency.domain.event.DocumentExpiryApproachingEvent;
import com.biddingagency.domain.member.entity.MemberDocument;
import com.biddingagency.domain.member.repository.MemberDocumentRepository;
import com.biddingagency.domain.notification.entity.NotificationLog;
import com.biddingagency.domain.notification.entity.NotificationType;
import com.biddingagency.domain.notification.repository.NotificationLogRepository;
import com.biddingagency.domain.setting.entity.SettingKey;
import com.biddingagency.domain.setting.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 서류 만료일 상태와 만료 메일 (CR-136, BID-MKT-013, BID-MKT-014)
 *
 * 만료일은 한국 날짜로 본다. 서버 시계의 시간대와 상관없이 "오늘" 은 Asia/Seoul 의 오늘이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentExpiryService {

    /** 만료일과 알림 시각의 기준 시간대 */
    public static final String SERVICE_ZONE = "Asia/Seoul";
    private static final ZoneId ZONE = ZoneId.of(SERVICE_ZONE);
    /** 알림 시점으로 받는 가장 긴 날 */
    private static final int MAX_ALERT_DAYS = 3650;

    private final MemberDocumentRepository documentRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final SystemSettingService systemSettingService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public enum ExpiryStatus { EXPIRED, SOON, OK, NONE }

    public record ExpiryDocument(String id, String category, String categoryLabel, String displayName,
                                 String fileName, LocalDate expiresOn, Integer daysLeft, ExpiryStatus status,
                                 LocalDateTime lastAlertedAt) {
    }

    public record Summary(int expired, int soon, int noDate, int total) {
    }

    /**
     * @param alertDays 알림 시점(POL-016), 큰 날부터. 꺼져 있으면 빈 목록
     * @param soonDays  "곧 만료" 기준(가장 큰 알림 날). 꺼져 있으면 null
     */
    public record ExpiryOverview(List<Integer> alertDays, Integer soonDays, LocalDate today, Summary summary,
                                 List<ExpiryDocument> documents) {
    }

    public LocalDate today() {
        return LocalDate.now(clock.withZone(ZONE));
    }

    /**
     * 알림 시점. 값이 비었거나 하나라도 읽을 수 없으면 꺼짐(빈 목록)으로 본다.
     * 잘못 넣은 값으로 엉뚱한 날에 메일이 나가는 것보다 보내지 않는 편이 낫다.
     */
    public List<Integer> alertDays() {
        return systemSettingService.findValue(SettingKey.DOC_EXPIRY_ALERT_DAYS).map(value -> {
            try {
                List<Integer> days = java.util.Arrays.stream(value.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Integer::parseInt)
                        .distinct()
                        .sorted(Comparator.reverseOrder())
                        .toList();
                if (days.isEmpty() || days.stream().anyMatch(d -> d < 0 || d > MAX_ALERT_DAYS)) {
                    log.warn("서류 만료 알림 시점이 범위를 벗어나 알림을 끕니다: {}", value);
                    return List.<Integer>of();
                }
                return days;
            } catch (NumberFormatException e) {
                log.warn("서류 만료 알림 시점을 읽지 못해 알림을 끕니다: {}", value);
                return List.<Integer>of();
            }
        }).orElse(List.of());
    }

    @Transactional(readOnly = true)
    public ExpiryOverview overview(UUID memberId) {
        List<Integer> alertDays = alertDays();
        Integer soonDays = alertDays.isEmpty() ? null : alertDays.get(0);
        LocalDate today = today();

        Map<UUID, LocalDateTime> lastAlerted = new HashMap<>();
        for (NotificationLog l : notificationLogRepository
                .findByRecipientIdAndNotificationTypeAndSuccessTrue(memberId, NotificationType.DOCUMENT_EXPIRY)) {
            if (l.getReferenceId() != null) lastAlerted.merge(l.getReferenceId(), l.getSentAt(),
                    (a, b) -> a.isAfter(b) ? a : b);
        }

        int expired = 0;
        int soon = 0;
        int noDate = 0;
        List<ExpiryDocument> docs = new java.util.ArrayList<>();
        for (MemberDocument d : documentRepository.findByMemberIdOrderByCreatedAtDesc(memberId)) {
            Integer daysLeft = d.getExpiresOn() == null ? null
                    : (int) ChronoUnit.DAYS.between(today, d.getExpiresOn());
            ExpiryStatus status = status(daysLeft, soonDays);
            switch (status) {
                case EXPIRED -> expired++;
                case SOON -> soon++;
                case NONE -> noDate++;
                default -> { }
            }
            docs.add(new ExpiryDocument(d.getId().toString(), d.getCategory().name(),
                    d.getCategory().getDisplayName(), d.getDisplayName(), d.getFileName(), d.getExpiresOn(),
                    daysLeft, status, lastAlerted.get(d.getId())));
        }
        docs.sort(Comparator.comparing(ExpiryDocument::expiresOn, Comparator.nullsLast(Comparator.naturalOrder())));
        return new ExpiryOverview(alertDays, soonDays, today, new Summary(expired, soon, noDate, docs.size()), docs);
    }

    static ExpiryStatus status(Integer daysLeft, Integer soonDays) {
        if (daysLeft == null) return ExpiryStatus.NONE;
        if (daysLeft < 0) return ExpiryStatus.EXPIRED;
        if (soonDays != null && daysLeft <= soonDays) return ExpiryStatus.SOON;
        return ExpiryStatus.OK;
    }

    /**
     * 만료일이 오늘부터 정확히 n일 뒤(n 은 알림 시점)인 서류마다 알림 이벤트를 낸다.
     * 같은 서류·만료일·시점은 알림 모듈이 멱등 키로 한 번만 보낸다(BIZ-030).
     *
     * @return 낸 이벤트 수
     */
    @Transactional(readOnly = true)
    public int publishDueAlerts() {
        List<Integer> alertDays = alertDays();
        if (alertDays.isEmpty()) {
            log.info("서류 만료 알림이 꺼져 있어 보내지 않습니다 ({})", SettingKey.DOC_EXPIRY_ALERT_DAYS);
            return 0;
        }
        LocalDate today = today();
        int published = 0;
        for (int days : alertDays) {
            LocalDate target = today.plusDays(days);
            for (MemberDocument d : documentRepository.findByExpiresOn(target)) {
                eventPublisher.publishEvent(new DocumentExpiryApproachingEvent(d.getId(), d.getMemberId(),
                        d.getDisplayName(), d.getCategory().getDisplayName(), d.getExpiresOn(), days));
                published++;
            }
        }
        log.info("서류 만료 알림 {}건 (오늘 {}, 시점 {})", published, today, alertDays);
        return published;
    }
}
