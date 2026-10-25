package com.biddingagency.controller.admin;

import com.biddingagency.domain.analysis.dto.BidAnalysisDto.Overview;
import com.biddingagency.domain.analysis.entity.BidDecision;
import com.biddingagency.domain.analysis.service.BidAnalysisService;
import com.biddingagency.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/bid-requests/{id}/bid-analysis")
@RequiredArgsConstructor
@Tag(name = "Admin - Bid Analysis", description = "CR-170 입찰 분석과 Bid/No-Bid 결정")
@PreAuthorize("hasRole('ADMIN')")
public class BidAnalysisAdminController {

    private final BidAnalysisService analysisService;

    @GetMapping
    @Operation(summary = "입찰 분석 조회", description = "지금 분석(가장 늦게 시작한 회차)과 서류 읽기 진행 여부")
    public ResponseEntity<Overview> overview(@PathVariable UUID id) {
        return ResponseEntity.ok(analysisService.overview(id));
    }

    @PostMapping("/run")
    @Operation(summary = "다시 분석", description = "분석 단계이고 분석 중이 아니며 서류 읽기가 끝났을 때만. 새 회차가 지금 분석이 된다")
    public ResponseEntity<Overview> run(@PathVariable UUID id,
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(analysisService.rerun(id, userDetails.getMember().getId()));
    }

    @PostMapping("/decision")
    @Operation(summary = "Bid/No-Bid 결정 저장", description = "조건부 Go 와 No-Go 는 note 가 필요하다. 회차가 없으면 결정만 기록한 회차를 만든다")
    public ResponseEntity<Overview> decide(@PathVariable UUID id,
                                           @Valid @RequestBody DecisionRequest request,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(analysisService.decide(id, request.decision(), request.note(),
                userDetails.getMember().getId()));
    }

    public record DecisionRequest(@NotNull BidDecision decision, @Size(max = 2000) String note) {
    }
}
