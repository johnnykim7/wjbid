package com.biddingagency.domain.bid.service;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.event.DeadlineApproachingEvent;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.setting.entity.SettingKey;
import com.biddingagency.domain.setting.service.AlertDaysSetting;
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
import java.util.List;

/**
 * 입찰 요청 마감 알림 (BID-NOTIFY-002, POL-009, CR-156)
 *
 * 남은 날은 한국 날짜로 센다. 마감 시각이 몇 시든 "오늘" 과 "마감일" 모두 Asia/Seoul 의 날짜다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeadlineAlertService {

    public static final String SERVICE_ZONE = "Asia/Seoul";
    private static final ZoneId ZONE = ZoneId.of(SERVICE_ZONE);
    /** 알림 시점으로 받는 가장 긴 날 */
    private static final int MAX_ALERT_DAYS = 365;

    /** 제출 전이라 마감 알림을 받는 상태 */
    static final List<BidRequestState> ACTIVE_STATES = List.of(
            BidRequestState.CREATED, BidRequestState.DOCS_PENDING,
            BidRequestState.DOCS_RECEIVED, BidRequestState.ANALYZING,
            BidRequestState.GENERATING, BidRequestState.REVIEW,
            BidRequestState.CONFIRMED);

    private final BidRequestRepository bidRequestRepository;
    private final SystemSettingService systemSettingService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /** 알림 시점, 큰 날부터. 꺼져 있으면 빈 목록 */
    public List<Integer> alertDays() {
        return systemSettingService.findValue(SettingKey.DEADLINE_ALERT_DAYS)
                .map(value -> AlertDaysSetting.parse(SettingKey.DEADLINE_ALERT_DAYS, value, MAX_ALERT_DAYS))
                .orElse(List.of());
    }

    /**
     * 마감일이 오늘부터 정확히 n일 뒤(n 은 알림 시점)인 진행 중 입찰 요청마다 알림 이벤트를 낸다.
     * 같은 요청, 마감일, 시점, 받는 사람은 알림 모듈이 멱등 키로 한 번만 보낸다(BIZ-012).
     *
     * @return 낸 이벤트 수
     */
    @Transactional(readOnly = true)
    public int publishDueAlerts() {
        List<Integer> alertDays = alertDays();
        if (alertDays.isEmpty()) {
            log.info("마감 알림이 꺼져 있어 보내지 않습니다 ({})", SettingKey.DEADLINE_ALERT_DAYS);
            return 0;
        }
        LocalDate today = LocalDate.now(clock.withZone(ZONE));
        int published = 0;
        for (BidRequest br : bidRequestRepository.findByStateInWithOpportunity(ACTIVE_STATES)) {
            Opportunity opp = br.getOpportunity();
            if (opp == null || opp.getResponseDeadline() == null) continue;
            int daysLeft = (int) ChronoUnit.DAYS.between(today, deadlineDate(opp.getResponseDeadline()));
            if (!alertDays.contains(daysLeft)) continue;
            eventPublisher.publishEvent(new DeadlineApproachingEvent(br.getId(), daysLeft, opp.getResponseDeadline()));
            published++;
        }
        log.info("마감 알림 {}건 (오늘 {}, 시점 {})", published, today, alertDays);
        return published;
    }

    /** JPA 는 마감 시각을 JVM 기본 시간대(시계의 시간대, ClockConfig) 벽시계로 읽으므로 그 시간대에서 서울 날짜로 바꾼다 */
    private LocalDate deadlineDate(LocalDateTime deadline) {
        return deadline.atZone(clock.getZone()).withZoneSameInstant(ZONE).toLocalDate();
    }
}
