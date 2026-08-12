package com.biddingagency.integration.samgov.scheduler;

import com.biddingagency.domain.event.CollectionFailedEvent;
import com.biddingagency.domain.event.OpportunitiesCollectedEvent;
import com.biddingagency.integration.samgov.client.OpportunityCollectorService;
import com.biddingagency.integration.samgov.quota.SamApiCallLogRepository;
import com.biddingagency.integration.samgov.quota.SamQuotaLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Opportunity Collection Scheduler
 *
 * Runs 4 times daily: 06:00, 12:00, 18:00, 23:00 KST
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpportunityCollectionScheduler {

    private final OpportunityCollectorService collectorService;
    private final ApplicationEventPublisher eventPublisher;
    /** CR-126: 수집 실패 시 HTTP status(401/403) 판별용 — CR-036 계측 재사용. */
    private final SamApiCallLogRepository samApiCallLogRepository;

    /** 스케줄 수집 on/off. SAM 일일 쿼터 보호용으로 운영에서 끌 수 있다. 수동 트리거는 영향 없음. */
    @Value("${app.sam-gov.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    // 타깃: 411TH CSB 단일 키워드만 수집 (CR-026).
    // SAM의 fullParentPathName은 '411TH CSB' 대문자가 정본. phrase 검색 + organization 후필터 양쪽에 동일 토큰 사용.
    private static final List<String> MAIN_KEYWORDS = List.of("411TH CSB");

    /**
     * Scheduled collection - runs at 06:00, 12:00, 18:00, 23:00 KST
     * Cron: "0 0 6,12,18,23 * * *"
     */
    @Scheduled(cron = "0 0 6,12,18,23 * * *", zone = "Asia/Seoul")
    public void collectOpportunities() {
        if (!schedulerEnabled) {
            log.info("====== Scheduled collection SKIPPED (app.sam-gov.scheduler.enabled=false) ======");
            return;
        }
        log.info("====== Starting scheduled opportunity collection at {} ======",
                LocalDateTime.now());

        int totalCollected = 0;
        int totalTargetNew = 0;
        int totalChanged = 0;
        int totalUnchanged = 0;
        int totalDuplicate = 0;
        int totalErrors = 0;

        // Collect last 30 days
        int daysBack = 30;

        // MAIN Keywords (411TH CSB - primary target)
        log.info("Collecting MAIN keywords...");
        for (String keyword : MAIN_KEYWORDS) {
            OpportunityCollectorService.CollectionResult result =
                    collectorService.collectByKeyword(keyword, daysBack);
            totalCollected += result.collected();
            totalTargetNew += result.targetNew();
            totalChanged += result.changed();
            totalUnchanged += result.unchanged();
            totalDuplicate += result.duplicate();
            totalErrors += result.errors();
        }

        // CR-009: totalFetched = 신규 + 변경 + 무변경, updatedCount = 실제 변경(CHANGED)만
        int totalFetched = totalCollected + totalChanged + totalUnchanged;
        log.info("====== Collection completed: {} new ({} target), {} changed, {} unchanged, {} duplicate(CR-042), {} errors ======",
                totalCollected, totalTargetNew, totalChanged, totalUnchanged, totalDuplicate, totalErrors);

        eventPublisher.publishEvent(new OpportunitiesCollectedEvent(
                null, totalCollected, totalTargetNew, totalChanged, totalFetched));

        // CR-126: 실패는 성공 이벤트와 분리해 발행한다.
        // OpportunitiesCollected의 소비자는 타깃 신규 0건이면 발송을 스킵하므로(CR-015),
        // 인증 실패로 0건인 상황이 정상 0건에 묻힌다. 2026-08-03 키 만료 9일 방치 사건의 원인.
        publishFailureIfAny(totalErrors);
    }

    /**
     * CR-126: 수집 회차에 오류가 있거나 SAM 인증 실패(401/403)가 감지되면 실패 이벤트를 발행한다.
     *
     * <p>수집 경로의 광범위 catch가 HTTP status를 유실하므로, status는 CR-036 계측
     * (sam_api_call_log, UTC 일자·endpoint=search)에서 읽는다. 계측 조회가 실패해도
     * 알림 자체는 나가야 하므로(그게 이 기능의 목적) 예외를 삼키고 authFailure=false로 발행한다.
     */
    private void publishFailureIfAny(int totalErrors) {
        Integer lastStatus = null;
        String lastErrorBody = null;
        try {
            var todayLog = samApiCallLogRepository
                    .findByCallDateAndEndpoint(LocalDate.now(Clock.systemUTC()), SamQuotaLogger.EP_SEARCH);
            if (todayLog.isPresent()) {
                lastStatus = todayLog.get().getLastStatus();
                lastErrorBody = todayLog.get().getLastErrorBody();
            }
        } catch (Exception e) {
            log.warn("CR-126: 수집 계측 조회 실패 - authFailure 판별 없이 진행: {}", e.getMessage());
        }

        boolean authFailure = lastStatus != null && (lastStatus == 401 || lastStatus == 403);

        if (totalErrors == 0 && !authFailure) {
            return; // 정상 회차 (신규 0건이어도 실패가 아니다)
        }

        log.error("Collection failed: errors={} lastStatus={} authFailure={} - admin notification 발행",
                totalErrors, lastStatus, authFailure);

        eventPublisher.publishEvent(new CollectionFailedEvent(
                String.join(",", MAIN_KEYWORDS), totalErrors, lastStatus, lastErrorBody, authFailure));
    }

    /**
     * Manual trigger method (can be called by controller)
     */
    public void triggerManualCollection(int daysBack) {
        log.info("Manual collection triggered for last {} days", daysBack);

        int totalCollected = 0;
        int totalChanged = 0;
        int totalUnchanged = 0;
        int totalDuplicate = 0;
        int totalErrors = 0;

        for (String keyword : MAIN_KEYWORDS) {
            OpportunityCollectorService.CollectionResult result =
                    collectorService.collectByKeyword(keyword, daysBack);
            totalCollected += result.collected();
            totalChanged += result.changed();
            totalUnchanged += result.unchanged();
            totalDuplicate += result.duplicate();
            totalErrors += result.errors();
        }

        log.info("Manual collection completed: {} new, {} changed, {} unchanged, {} duplicate(CR-042), {} errors",
                totalCollected, totalChanged, totalUnchanged, totalDuplicate, totalErrors);
    }
}
