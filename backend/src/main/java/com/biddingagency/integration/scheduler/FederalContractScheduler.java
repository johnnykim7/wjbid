package com.biddingagency.integration.scheduler;

import com.biddingagency.domain.federal.entity.FederalContractSyncRun;
import com.biddingagency.domain.federal.entity.FederalSyncMode;
import com.biddingagency.domain.federal.service.FederalContractSyncService;
import com.biddingagency.domain.federal.service.FederalSettings;
import com.biddingagency.domain.federal.service.MemberContractService;
import com.biddingagency.domain.federal.service.SyncAlreadyRunningException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 미 연방 계약 매일 받기(POL-032, 설정 app.federal-contract.cron)와 계약 끝 메일 확인(POL-035, 설정 app.contract-end.cron).
 * 시각은 한국 시각 (CR-142).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FederalContractScheduler {

    private final FederalContractSyncService syncService;
    private final MemberContractService memberContractService;

    @Scheduled(cron = "${app.federal-contract.cron:0 0 5 * * *}", zone = FederalSettings.SERVICE_ZONE)
    public void syncDaily() {
        try {
            FederalContractSyncRun run = syncService.start(FederalSyncMode.DAILY, FederalContractSyncRun.BY_SCHEDULER);
            syncService.execute(run.getId());
        } catch (SyncAlreadyRunningException e) {
            log.info("미 연방 계약 받기가 돌고 있어 이번 매일 받기를 건너뜁니다");
        } catch (Exception e) {
            log.error("미 연방 계약 매일 받기 실패", e);
        }
    }

    @Scheduled(cron = "${app.contract-end.cron:0 0 9 * * *}", zone = FederalSettings.SERVICE_ZONE)
    public void checkContractEnds() {
        try {
            memberContractService.publishDueAlerts();
        } catch (Exception e) {
            log.error("계약 끝 알림 확인 실패", e);
        }
    }
}
