package com.biddingagency.controller;

import com.biddingagency.domain.bid.dto.BidRequestDto;
import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.service.BidFSMService;
import com.biddingagency.domain.bid.service.BidRequestService;
import com.biddingagency.domain.document.service.DocumentVersionService;
import com.biddingagency.dto.BidRequestCreateRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/bid-requests")
@RequiredArgsConstructor
@Tag(name = "Bid Requests", description = "입찰 요청 관리")
public class BidRequestController {

    private final BidRequestService bidRequestService;
    private final BidFSMService fsmService;
    private final DocumentVersionService documentVersionService;

    @PostMapping
    @Operation(summary = "입찰 요청 생성")
    public ResponseEntity<BidRequestDto> createBidRequest(
            @Valid @RequestBody BidRequestCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        BidRequest bidRequest = bidRequestService.createBidRequest(
                userDetails.getMember().getId(),
                request.getOpportunityId(),
                userDetails.getMember().getId(),
                userDetails.getUsername()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(BidRequestDto.from(bidRequest));
    }

    @GetMapping("/my")
    @Operation(summary = "내 입찰 요청 목록")
    public ResponseEntity<Page<BidRequestDto>> getMyBidRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<BidRequestDto> page = bidRequestService.findByMember(
                userDetails.getMember().getId(), pageable)
                .map(BidRequestDto::from);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "입찰 요청 상세")
    public ResponseEntity<BidRequestDto> getBidRequest(@PathVariable UUID id) {
        BidRequest bidRequest = bidRequestService.findByIdWithDetails(id);
        return ResponseEntity.ok(BidRequestDto.withHistory(bidRequest));
    }

    @PatchMapping("/{id}/state")
    @Operation(summary = "상태 전환")
    public ResponseEntity<BidRequestDto> transitionState(
            @PathVariable UUID id,
            @Valid @RequestBody StateTransitionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        BidRequest bidRequest = fsmService.transition(
                id, request.getToState(),
                userDetails.getMember().getId(),
                userDetails.getUsername(),
                request.getNotes()
        );
        return ResponseEntity.ok(BidRequestDto.withHistory(bidRequest));
    }

    @GetMapping("/{id}/next-states")
    @Operation(summary = "가능한 다음 상태 목록")
    public ResponseEntity<List<com.biddingagency.domain.bid.entity.BidRequestState>> getValidNextStates(@PathVariable UUID id) {
        BidRequest bidRequest = bidRequestService.findById(id);
        return ResponseEntity.ok(fsmService.getValidNextStates(bidRequest.getState()));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "상태 전환 이력")
    public ResponseEntity<List<?>> getStateHistory(@PathVariable UUID id) {
        BidRequest bidRequest = bidRequestService.findById(id);
        return ResponseEntity.ok(bidRequest.getStateHistory());
    }

    @GetMapping("/{id}/documents")
    @Operation(summary = "입찰 요청의 문서 목록 (최신 버전 내용 포함)")
    public ResponseEntity<List<java.util.Map<String, Object>>> getDocuments(@PathVariable UUID id) {
        return ResponseEntity.ok(documentVersionService.getDocumentSummaries(id));
    }
}
