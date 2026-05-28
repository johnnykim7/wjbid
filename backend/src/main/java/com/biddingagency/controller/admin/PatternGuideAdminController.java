package com.biddingagency.controller.admin;

import com.biddingagency.domain.rfp.dto.PatternGuideDto;
import com.biddingagency.domain.rfp.dto.PatternGuideEditRequest;
import com.biddingagency.domain.rfp.entity.IndustryType;
import com.biddingagency.domain.rfp.service.PatternExtractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 관리자 패턴 가이드 추출/조회/편집 Controller (CR-013) */
@Slf4j
@RestController
@RequestMapping("/admin/pattern-guides")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Pattern Guides", description = "슬롯별 패턴 추출 + 가이드 조회/편집 (CR-013)")
public class PatternGuideAdminController {

    private final PatternExtractionService patternExtractionService;

    @GetMapping
    @Operation(summary = "전체 슬롯 가이드 목록")
    public ResponseEntity<List<PatternGuideDto>> list() {
        return ResponseEntity.ok(patternExtractionService.listGuides());
    }

    @GetMapping("/{slotCode}")
    @Operation(summary = "슬롯 가이드 상세")
    public ResponseEntity<PatternGuideDto> get(@PathVariable String slotCode) {
        PatternGuideDto guide = patternExtractionService.getGuide(slotCode);
        return guide != null ? ResponseEntity.ok(guide) : ResponseEntity.noContent().build();
    }

    @PostMapping("/{slotCode}/extract")
    @Operation(summary = "슬롯 단위 패턴 추출 트리거 (BIZ-016: 2건 이상)")
    public ResponseEntity<PatternGuideDto> extract(
            @PathVariable String slotCode,
            @RequestParam(value = "industryType", required = false) IndustryType industryType) {
        return ResponseEntity.accepted().body(patternExtractionService.triggerExtraction(slotCode, industryType));
    }

    @PutMapping("/{slotCode}")
    @Operation(summary = "가이드 수동 편집 (source=HUMAN_EDITED, 자동추출 보호)")
    public ResponseEntity<PatternGuideDto> update(
            @PathVariable String slotCode,
            @RequestBody PatternGuideEditRequest req) {
        return ResponseEntity.ok(patternExtractionService.updateGuideManually(slotCode, req));
    }
}
