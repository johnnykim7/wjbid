package com.biddingagency.controller.admin;

import com.biddingagency.domain.federal.dto.FederalContractResponses.SyncRunRow;
import com.biddingagency.domain.federal.dto.FederalContractResponses.SyncStarted;
import com.biddingagency.domain.federal.entity.FederalContractSyncRun;
import com.biddingagency.domain.federal.entity.FederalSyncMode;
import com.biddingagency.domain.federal.exception.FederalContractException;
import com.biddingagency.domain.federal.service.FederalContractSyncLauncher;
import com.biddingagency.domain.federal.service.FederalContractSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 미 연방 계약 받기 (CR-142, T3-2 B-5). ADMIN 만(/admin/**).
 */
@RestController
@RequestMapping("/admin/federal-contracts")
@RequiredArgsConstructor
@Tag(name = "FederalContractAdmin", description = "미 연방 계약 받기 (CR-142)")
public class FederalContractAdminController {

    private final FederalContractSyncLauncher launcher;
    private final FederalContractSyncService syncService;

    @PostMapping("/sync")
    @Operation(summary = "받기 바로 실행", description = "mode FULL 또는 DAILY. 뒤에서 돌리고 202 와 회차 id. 돌고 있으면 409")
    public ResponseEntity<SyncStarted> sync(@RequestBody(required = false) Map<String, String> body) {
        FederalSyncMode mode;
        try {
            mode = FederalSyncMode.valueOf(body == null ? "" : String.valueOf(body.get("mode")));
        } catch (IllegalArgumentException e) {
            throw FederalContractException.badRequest("FEDERAL_SYNC_MODE_INVALID", "mode 는 FULL 이나 DAILY 여야 합니다");
        }
        FederalContractSyncRun run = launcher.launch(mode, FederalContractSyncRun.BY_ADMIN);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new SyncStarted(run.getId(), run.getMode()));
    }

    @GetMapping("/sync-runs")
    @Operation(summary = "최근 받기 회차", description = "최근 시작순, size 기본 20, 최대 100")
    public ResponseEntity<List<SyncRunRow>> runs(@RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(syncService.recentRuns(size));
    }
}
