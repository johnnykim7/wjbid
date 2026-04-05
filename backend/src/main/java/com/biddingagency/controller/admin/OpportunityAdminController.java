package com.biddingagency.controller.admin;

import com.biddingagency.domain.opportunity.dto.OpportunityAdminDto;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityAnalysis;
import com.biddingagency.domain.opportunity.entity.OpportunityAttachment;
import com.biddingagency.domain.opportunity.repository.OpportunityAttachmentRepository;
import com.biddingagency.domain.opportunity.service.OpportunityAnalysisService;
import com.biddingagency.domain.opportunity.service.OpportunityService;
import com.biddingagency.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

/**
 * 관리자 공고 관리 Controller (CR-003)
 */
@Slf4j
@RestController
@RequestMapping("/admin/opportunities")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Opportunities", description = "공고 관리 (사전 분석, 노출 승인)")
public class OpportunityAdminController {

    private final OpportunityService opportunityService;
    private final OpportunityAnalysisService analysisService;
    private final OpportunityAttachmentRepository attachmentRepository;

    @GetMapping
    @Operation(summary = "공고 목록 (관리자)", description = "첨부파일 수, 분석 상태, 노출 상태 포함")
    public ResponseEntity<Page<OpportunityAdminDto>> listOpportunities(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<OpportunityAdminDto> page = opportunityService.findAllActive(pageable)
                .map(opp -> {
                    long attachmentCount = attachmentRepository.countByOpportunityId(opp.getId());
                    OpportunityAnalysis analysis = analysisService.findByOpportunityId(opp.getId()).orElse(null);
                    return OpportunityAdminDto.fromList(opp, attachmentCount, analysis);
                });
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "공고 상세 (관리자)", description = "분석 결과 포함")
    public ResponseEntity<OpportunityAdminDto> getOpportunity(@PathVariable UUID id) {
        Opportunity opp = opportunityService.findById(id);
        long attachmentCount = attachmentRepository.countByOpportunityId(id);
        OpportunityAnalysis analysis = analysisService.findByOpportunityId(id).orElse(null);
        return ResponseEntity.ok(OpportunityAdminDto.from(opp, attachmentCount, analysis));
    }

    @GetMapping("/{id}/analysis")
    @Operation(summary = "사전 분석 결과 조회")
    public ResponseEntity<OpportunityAnalysis> getAnalysis(@PathVariable UUID id) {
        OpportunityAnalysis analysis = analysisService.findByOpportunityId(id)
                .orElseThrow(() -> new IllegalArgumentException("분석 결과 없음: " + id));
        return ResponseEntity.ok(analysis);
    }

    @PostMapping("/{id}/attachments")
    @Operation(summary = "첨부파일 수동 업로드")
    public ResponseEntity<Map<String, String>> uploadAttachment(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        Opportunity opp = opportunityService.findById(id);

        // 첨부파일 메타데이터 저장 (실제 파일 저장은 MinIO 연동 후)
        OpportunityAttachment attachment = OpportunityAttachment.builder()
                .opportunity(opp)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .sourceUrl("admin-upload")
                .build();
        attachment.markLinkOnly(); // MinIO 연동 전까지 임시
        attachmentRepository.save(attachment);

        log.info("관리자 첨부파일 업로드: opportunityId={}, fileName={}", id, file.getOriginalFilename());

        return ResponseEntity.ok(Map.of(
                "status", "UPLOADED",
                "fileName", file.getOriginalFilename() != null ? file.getOriginalFilename() : "",
                "opportunityId", id.toString()
        ));
    }

    @PostMapping("/{id}/analyze")
    @Operation(summary = "사전 분석 트리거 (수동)")
    public ResponseEntity<Map<String, String>> triggerAnalysis(@PathVariable UUID id) {
        opportunityService.findById(id); // existence check
        analysisService.triggerAnalysis(id);
        log.info("사전 분석 트리거: opportunityId={}", id);
        return ResponseEntity.accepted().body(Map.of(
                "status", "ACCEPTED",
                "message", "사전 분석이 시작되었습니다.",
                "opportunityId", id.toString()
        ));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "공고 노출 승인 (HIDDEN → VISIBLE)")
    public ResponseEntity<Map<String, String>> approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Opportunity opp = opportunityService.approve(id, userDetails.getMember().getId());
        log.info("공고 노출 승인: opportunityId={}, visibility={}", id, opp.getVisibility());
        return ResponseEntity.ok(Map.of(
                "status", "APPROVED",
                "visibility", opp.getVisibility().name(),
                "opportunityId", id.toString()
        ));
    }

    @PostMapping("/{id}/hide")
    @Operation(summary = "공고 비노출 (VISIBLE → HIDDEN)")
    public ResponseEntity<Map<String, String>> hide(@PathVariable UUID id) {
        Opportunity opp = opportunityService.hide(id);
        log.info("공고 비노출: opportunityId={}, visibility={}", id, opp.getVisibility());
        return ResponseEntity.ok(Map.of(
                "status", "HIDDEN",
                "visibility", opp.getVisibility().name(),
                "opportunityId", id.toString()
        ));
    }

    @PatchMapping("/{id}/analysis")
    @Operation(summary = "사전 분석 결과 보정 (관리자 수동)")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, String>> updateAnalysis(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> summaryJson = (Map<String, Object>) body.get("summaryJson");
        Map<String, Object> documentFormatsJson = (Map<String, Object>) body.get("documentFormatsJson");
        Map<String, Object> requiredDocumentsJson = (Map<String, Object>) body.get("requiredDocumentsJson");
        Map<String, Object> llmPromptPresetJson = (Map<String, Object>) body.get("llmPromptPresetJson");

        analysisService.updateAnalysisResult(id, summaryJson, documentFormatsJson, requiredDocumentsJson, llmPromptPresetJson);
        log.info("사전 분석 결과 보정: opportunityId={}", id);
        return ResponseEntity.ok(Map.of(
                "status", "UPDATED",
                "opportunityId", id.toString()
        ));
    }
}
