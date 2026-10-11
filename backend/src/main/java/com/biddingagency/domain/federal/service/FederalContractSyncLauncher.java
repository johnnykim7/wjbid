package com.biddingagency.domain.federal.service;

import com.biddingagency.domain.federal.entity.FederalContractSyncRun;
import com.biddingagency.domain.federal.entity.FederalSyncMode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * 관리자가 누른 받기를 뒤에서 돌린다 (CR-142, BID-MKT-027). 첫 전체 받기는 10분쯤 걸린다.
 */
@Component
@RequiredArgsConstructor
public class FederalContractSyncLauncher {

    private final FederalContractSyncService syncService;
    private final SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("federal-sync-");

    /** 회차를 만들고 뒤에서 돌린다. 돌고 있으면 SyncAlreadyRunningException */
    public FederalContractSyncRun launch(FederalSyncMode mode, String triggeredBy) {
        FederalContractSyncRun run = syncService.start(mode, triggeredBy);
        executor.execute(() -> syncService.execute(run.getId()));
        return run;
    }
}
