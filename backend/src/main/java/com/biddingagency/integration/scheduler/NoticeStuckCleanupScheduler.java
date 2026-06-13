package com.biddingagency.integration.scheduler;

import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.notice.repository.NoticeRepository;
import com.biddingagency.domain.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CR-039: 공고문 한글화/분석 stuck 자동 정리 스케줄러.
 *
 * ANALYZING 상태로 멈춰(stuck) 무한 폴링되는 공고문을 임계 시간 초과 시 자동 FAILED 처리한다.
 * - 임계: notice.analysis.stuck-threshold-minutes (POL-012, 기본 60분)
 * - 주기: notice.analysis.scheduler-interval-ms (기본 600000=10분)
 * 수동 강제 중단(NoticeService.cancelAnalysis)과 동일 경로 — Aimbase 취소 best-effort 후 FAILED 전환.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeStuckCleanupScheduler {

    private final NoticeRepository noticeRepository;
    private final NoticeService noticeService;

    @Value("${app.notice.analysis.stuck-threshold-minutes:60}")
    private long stuckThresholdMinutes;

    /**
     * fixedDelay: 이전 실행 완료 후 interval 만큼 대기 후 재실행(겹침 방지).
     * 임계 초과 ANALYZING 건을 조회해 각각 강제 중단 경로로 FAILED 전환.
     */
    @Scheduled(fixedDelayString = "${app.notice.analysis.scheduler-interval-ms:600000}")
    public void cleanupStuckAnalyzing() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(stuckThresholdMinutes);
        List<Notice> stuck = noticeRepository.findStuckAnalyzing(threshold);
        if (stuck.isEmpty()) {
            return;
        }
        log.warn("[공고문] stuck 자동 정리 시작: {}건 (임계 {}분 초과)", stuck.size(), stuckThresholdMinutes);
        String reason = "stuck 자동 정리 (분석 시작 후 " + stuckThresholdMinutes + "분 초과)";
        for (Notice notice : stuck) {
            try {
                noticeService.cancelAnalysis(notice.getId(), reason);
            } catch (Exception e) {
                // 한 건 실패가 나머지 정리를 막지 않도록 격리
                log.error("[공고문] stuck 정리 실패(건너뜀): noticeId={}", notice.getId(), e);
            }
        }
    }
}
