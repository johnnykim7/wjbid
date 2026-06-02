package com.biddingagency.controller;

import com.biddingagency.domain.notice.service.NoticeService;
import com.biddingagency.domain.opportunity.dto.OpportunityDto;
import com.biddingagency.domain.opportunity.entity.OpportunityAttachment;
import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import com.biddingagency.domain.opportunity.repository.OpportunityAttachmentRepository;
import com.biddingagency.domain.opportunity.repository.OpportunityRequirementItemRepository;
import com.biddingagency.domain.opportunity.service.OpportunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Opportunity Controller (고객) — CR-016.
 *
 * 고객 노출 단위 = 공고문(Notice, 노출된 것만). id = noticeId.
 * 원본(Opportunity)은 관리자 선별 풀이라 고객에 직접 노출하지 않음.
 */
@Slf4j
@RestController
@RequestMapping("/opportunities")
@RequiredArgsConstructor
@Tag(name = "Opportunities", description = "고객 공고문 조회")
public class OpportunityController {

    private final OpportunityService opportunityService;
    private final NoticeService noticeService;
    private final OpportunityRequirementItemRepository requirementItemRepository;
    private final OpportunityAttachmentRepository attachmentRepository;

    /**
     * 노출 공고문 목록 (CR-016)
     */
    @GetMapping
    @Operation(summary = "공고문 목록", description = "노출(VISIBLE) 공고문, 한글화 제목 포함. CR-009: 최신순 + 마감 공고 기본 제외")
    public ResponseEntity<Page<OpportunityDto>> listOpportunities(
            @RequestParam(defaultValue = "false") boolean includeExpired,
            // 정렬은 쿼리에 고정(게시일 DESC, 2차 수집순 DESC) — Pageable sort 미지정
            @PageableDefault(size = 20) Pageable pageable) {
        Page<OpportunityDto> notices = noticeService.findVisible(includeExpired, pageable)
                .map(OpportunityDto::fromNotice);
        return ResponseEntity.ok(notices);
    }

    /**
     * 공고문 상세 (id = noticeId)
     */
    @GetMapping("/{id}")
    @Operation(summary = "공고문 상세", description = "노출 공고문 상세 + 한글화 결과")
    public ResponseEntity<OpportunityDto> getOpportunity(@PathVariable UUID id) {
        return noticeService.findVisibleById(id)
                .map(OpportunityDto::fromNotice)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 공고문 검색 (한글/원문 제목)
     */
    @GetMapping("/search")
    @Operation(summary = "공고문 검색", description = "노출 공고문 제목 검색. CR-009: 최신순 + 마감 공고 기본 제외")
    public ResponseEntity<Page<OpportunityDto>> searchOpportunities(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "false") boolean includeExpired,
            // 정렬은 쿼리에 고정(게시일 DESC, 2차 수집순 DESC) — Pageable sort 미지정
            @PageableDefault(size = 20) Pageable pageable) {
        Page<OpportunityDto> notices = noticeService.searchVisible(keyword, includeExpired, pageable)
                .map(OpportunityDto::fromNotice);
        return ResponseEntity.ok(notices);
    }

    /**
     * Get opportunities near deadline
     */
    @GetMapping("/near-deadline")
    @Operation(summary = "Near deadline", description = "Get opportunities approaching deadline")
    public ResponseEntity<List<OpportunityDto>> nearDeadline(
            @RequestParam(defaultValue = "7") int days) {
        log.debug("Fetching opportunities near deadline (within {} days)", days);
        List<OpportunityDto> opportunities = noticeService.findVisibleNearDeadline(days)
                .stream().map(OpportunityDto::fromNotice).toList();
        return ResponseEntity.ok(opportunities);
    }

    /**
     * Get opportunity requirements
     */
    @GetMapping("/{id}/requirements")
    @Operation(summary = "공고 요구사항 목록")
    public ResponseEntity<List<OpportunityRequirementItem>> getRequirements(@PathVariable UUID id) {
        opportunityService.findById(id); // existence check
        return ResponseEntity.ok(requirementItemRepository.findByOpportunityId(id));
    }

    /**
     * Get opportunity attachments
     */
    @GetMapping("/{id}/attachments")
    @Operation(summary = "공고 첨부파일 목록")
    public ResponseEntity<List<OpportunityAttachment>> getAttachments(@PathVariable UUID id) {
        opportunityService.findById(id); // existence check
        return ResponseEntity.ok(attachmentRepository.findByOpportunityId(id));
    }

    /**
     * Qualification check (async via Aimbase)
     */
    @PostMapping("/{id}/qualification-check")
    @Operation(summary = "AI 자격 진단 요청")
    public ResponseEntity<java.util.Map<String, String>> qualificationCheck(@PathVariable UUID id) {
        opportunityService.findById(id); // existence check
        // TODO: Aimbase 연동 후 실제 AI 자격 진단 구현
        return ResponseEntity.accepted().body(java.util.Map.of(
                "status", "ACCEPTED",
                "message", "Qualification check requested. Results will be available shortly.",
                "opportunityId", id.toString()
        ));
    }

    /**
     * Get recently posted opportunities
     */
    @GetMapping("/recent")
    @Operation(summary = "Recently posted", description = "Get recently posted opportunities")
    public ResponseEntity<List<OpportunityDto>> recentlyPosted(
            @RequestParam(defaultValue = "30") int days) {
        log.debug("Fetching opportunities posted in last {} days", days);
        List<OpportunityDto> opportunities = noticeService.findVisibleRecentlyPosted(days)
                .stream().map(OpportunityDto::fromNotice).toList();
        return ResponseEntity.ok(opportunities);
    }
}
