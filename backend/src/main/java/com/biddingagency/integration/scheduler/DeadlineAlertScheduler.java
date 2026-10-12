package com.biddingagency.integration.scheduler;

import com.biddingagency.domain.bid.service.DeadlineAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 입찰 요청 마감 알림 확인 (BID-NOTIFY-002, CR-156). 확인 시각은 설정 app.deadline-alert.cron(한국 시각),
 * 며칠 전에 보낼지는 DB 설정 DEADLINE_ALERT_DAYS (POL-009).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadlineAlertScheduler {

    private final DeadlineAlertService deadlineAlertService;

    @Scheduled(cron = "${app.deadline-alert.cron:0 0 9 * * *}", zone = DeadlineAlertService.SERVICE_ZONE)
    public void checkDeadlines() {
        try {
            deadlineAlertService.publishDueAlerts();
        } catch (Exception e) {
            log.error("마감 알림 확인 실패", e);
        }
    }
}
