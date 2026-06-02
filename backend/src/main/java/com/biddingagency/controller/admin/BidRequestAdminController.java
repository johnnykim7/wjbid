package com.biddingagency.controller.admin;

import com.biddingagency.domain.bid.dto.BidRequestDto;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.service.BidFSMService;
import com.biddingagency.domain.bid.service.BidRequestService;
import com.biddingagency.domain.document.entity.DocumentType;
import com.biddingagency.dto.StateTransitionRequest;
import com.biddingagency.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bid Request Admin Controller
 *
 * Admin-only bid request management
 */
@Slf4j
@RestController
@RequestMapping("/admin/bid-requests")
@RequiredArgsConstructor
@Tag(name = "Admin - Bid Requests", description = "Admin bid request management")
@PreAuthorize("hasRole('ADMIN')")
public class BidRequestAdminController {

    private final BidRequestService bidRequestService;
    private final BidFSMService fsmService;

    /**
     * Get all bid requests by state
     */
    @GetMapping
    @Operation(summary = "List bid requests", description = "Get bid requests filtered by state (state 없으면 전체)")
    public ResponseEntity<Page<BidRequestDto>> listBidRequests(
            @RequestParam(required = false) BidRequestState state,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching bid requests, state filter: {}", state);
        // state==null이면 전체 조회 (이전엔 CREATED로 잘못 한정 → '전체' 탭이 비던 버그)
        return ResponseEntity.ok(bidRequestService.findAsDto(state, pageable));
    }

    /**
     * Get dashboard statistics
     */
    @GetMapping("/stats")
    @Operation(summary = "Dashboard stats", description = "Get bid request statistics by state")
    public ResponseEntity<Map<String, Long>> getStats() {
        Map<String, Long> stats = bidRequestService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get bid requests requiring client action
     */
    @GetMapping("/requiring-client-action")
    @Operation(summary = "Requiring client action", description = "Get bid requests waiting for client")
    public ResponseEntity<List<BidRequestDto>> getRequestsRequiringClientAction() {
        return ResponseEntity.ok(bidRequestService.getRequestsRequiringClientActionAsDto());
    }

    /**
     * Get bid request detail — CR-024: notice 매핑(noticeId/displayTitle) + 이력 포함 DTO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get bid request detail", description = "Admin bid request detail with state history")
    public ResponseEntity<BidRequestDto> getBidRequest(@PathVariable UUID id) {
        return ResponseEntity.ok(bidRequestService.findByIdAsDto(id));
    }

    /**
     * Transition bid request state
     */
    @PostMapping("/{id}/transition")
    @Operation(summary = "Transition state", description = "Change bid request state (FSM)")
    public ResponseEntity<BidRequestDto> transitionState(
            @PathVariable UUID id,
            @Valid @RequestBody StateTransitionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("Transitioning bid request {} to state {} by user {}",
                id, request.getToState(), userDetails.getUsername());

        // Validate if transitioning to CONFIRMED
        if (request.getToState() == BidRequestState.CONFIRMED) {
            fsmService.validateReadyForSubmission(id);
        }

        fsmService.transition(
                id,
                request.getToState(),
                userDetails.getMember().getId(),
                userDetails.getUsername(),
                request.getNotes()
        );

        // 전이 후 최신 상태를 DTO로 재조회 (Entity 직렬화 금지)
        return ResponseEntity.ok(bidRequestService.findByIdAsDto(id));
    }

    /**
     * CR-017 ②: 개별 문서 재생성 — REVIEW 상태에서 특정 문서 타입만 다시 생성.
     * 전체 재생성(REVIEW→GENERATING 전이)과 달리 FSM 상태는 REVIEW 유지, 결과는 새 버전으로 누적.
     */
    @PostMapping("/{id}/documents/{documentType}/regenerate")
    @Operation(summary = "Regenerate single document",
            description = "Regenerate one document type (REVIEW state only); keeps state, adds a new version")
    public ResponseEntity<Map<String, Object>> regenerateDocument(
            @PathVariable UUID id,
            @PathVariable DocumentType documentType,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("Regenerating document {} for bid request {} by {}",
                documentType, id, userDetails.getUsername());

        fsmService.regenerateDocument(id, documentType);

        return ResponseEntity.accepted().body(Map.of(
                "bidRequestId", id.toString(),
                "documentType", documentType.name(),
                "message", documentType.name() + " 문서 재생성을 시작했습니다. 완료 시 알림이 발송됩니다."
        ));
    }

    /**
     * Assign bid request to user
     */
    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign to user", description = "Assign bid request to a team member")
    public ResponseEntity<BidRequestDto> assignToUser(
            @PathVariable UUID id,
            @RequestParam UUID userId) {
        log.info("Assigning bid request {} to user {}", id, userId);

        bidRequestService.assignTo(id, userId);

        return ResponseEntity.ok(bidRequestService.findByIdAsDto(id));
    }

    /**
     * Get bid requests assigned to me
     */
    @GetMapping("/assigned-to-me")
    @Operation(summary = "Assigned to me", description = "Get bid requests assigned to current user")
    public ResponseEntity<Page<BidRequestDto>> getMyAssignedRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(bidRequestService.findByAssignedToAsDto(
                userDetails.getMember().getId(), pageable));
    }
}
