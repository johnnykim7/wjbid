package com.biddingagency.integration.scheduler;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.event.DeadlineApproachingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 마감일 임박 알림 스케줄러
 * 매일 09:00 KST에 D-7, D-3, D-1 알림 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadlineAlertScheduler {

    private final BidRequestRepository bidRequestRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void checkDeadlines() {
        log.info("Checking approaching deadlines...");

        // 활성 상태 (SUBMITTED, CLOSED 아닌) 입찰만 조회
        List<BidRequestState> activeStates = List.of(
                BidRequestState.CREATED, BidRequestState.DOCS_PENDING,
                BidRequestState.DOCS_RECEIVED, BidRequestState.ANALYZING,
                BidRequestState.GENERATING, BidRequestState.REVIEW,
                BidRequestState.CONFIRMED
        );

        List<BidRequest> requests = bidRequestRepository.findByStateIn(activeStates);
        for (BidRequest br : requests) {
            if (br.getOpportunity() == null || br.getOpportunity().getResponseDeadline() == null) continue;

            LocalDateTime deadline = br.getOpportunity().getResponseDeadline();
            long daysUntil = ChronoUnit.DAYS.between(LocalDateTime.now(), deadline);

            if (daysUntil == 7 || daysUntil == 3 || daysUntil == 1) {
                log.info("Deadline approaching: bidRequest={}, days={}", br.getId(), daysUntil);
                eventPublisher.publishEvent(
                        new DeadlineApproachingEvent(br.getId(), (int) daysUntil, deadline));
            }
        }

        log.info("Deadline check completed");
    }
}
