package com.biddingagency.controller;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.repository.BidDocumentRepository;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * FlowGuard Validation API — 읽기 전용 상태 조회.
 * FlowGuard Probe가 비즈니스 시나리오 실행 결과를 검증하기 위해 사용한다.
 */
@RestController
@RequestMapping("/validation")
@RequiredArgsConstructor
public class ValidationController {

    private final BidRequestRepository bidRequestRepository;
    private final OpportunityRepository opportunityRepository;
    private final BidDocumentRepository bidDocumentRepository;

    /**
     * GET /validation/bid-requests/{id}
     * 특정 BidRequest 상태 조회
     */
    @GetMapping("/bid-requests/{id}")
    public Map<String, Object> getBidRequest(@PathVariable UUID id) {
        Optional<BidRequest> opt = bidRequestRepository.findByIdWithDetails(id);
        if (opt.isEmpty()) {
            return Map.of("bidRequests", List.of());
        }
        BidRequest br = opt.get();
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("bidRequestId", br.getId().toString());
        item.put("state", br.getState().name());
        item.put("stateDisplay", br.getStateDisplay());
        item.put("memberId", br.getMember().getId().toString());
        item.put("opportunityId", br.getOpportunity().getId().toString());
        item.put("canEditDocuments", br.canEditDocuments());
        item.put("requiresClientAction", br.requiresClientAction());
        item.put("createdAt", br.getCreatedAt() != null ? br.getCreatedAt().toString() : null);
        return Map.of("bidRequests", List.of(item));
    }

    /**
     * GET /validation/bid-requests?state={state}
     * 상태별 BidRequest 목록 조회
     */
    @GetMapping("/bid-requests")
    public Map<String, Object> listBidRequests(
            @RequestParam(required = false) String state) {
        List<BidRequest> list;
        if (state != null && !state.isBlank()) {
            try {
                var s = com.biddingagency.domain.bid.entity.BidRequestState.valueOf(state);
                list = bidRequestRepository.findByStateIn(List.of(s));
            } catch (IllegalArgumentException e) {
                return Map.of("bidRequests", List.of(), "error", "Invalid state: " + state);
            }
        } else {
            list = bidRequestRepository.findAll();
        }
        List<Map<String, Object>> items = list.stream().map(br -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("bidRequestId", br.getId().toString());
            item.put("state", br.getState().name());
            item.put("stateDisplay", br.getStateDisplay());
            item.put("createdAt", br.getCreatedAt() != null ? br.getCreatedAt().toString() : null);
            return item;
        }).toList();
        return Map.of("bidRequests", items);
    }

    /**
     * GET /validation/opportunities/{id}
     * 특정 Opportunity 조회
     */
    @GetMapping("/opportunities/{id}")
    public Map<String, Object> getOpportunity(@PathVariable UUID id) {
        Optional<Opportunity> opt = opportunityRepository.findById(id);
        if (opt.isEmpty()) {
            return Map.of("opportunities", List.of());
        }
        Opportunity o = opt.get();
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("opportunityId", o.getId().toString());
        item.put("noticeId", o.getNoticeId());
        item.put("title", o.getTitle());
        item.put("active", o.getActive());
        item.put("responseDeadline", o.getResponseDeadline() != null ? o.getResponseDeadline().toString() : null);
        item.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
        return Map.of("opportunities", List.of(item));
    }

    /**
     * GET /validation/bid-documents?bidRequestId={bidRequestId}
     * BidRequest의 문서 목록 조회
     */
    @GetMapping("/bid-documents")
    public Map<String, Object> listBidDocuments(
            @RequestParam(required = false) UUID bidRequestId) {
        List<BidDocument> list;
        if (bidRequestId != null) {
            list = bidDocumentRepository.findByBidRequestId(bidRequestId);
        } else {
            list = bidDocumentRepository.findAll();
        }
        List<Map<String, Object>> items = list.stream().map(doc -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("documentId", doc.getId().toString());
            item.put("documentType", doc.getDocumentType().name());
            item.put("status", doc.getStatus().name());
            item.put("currentVersionNo", doc.getCurrentVersionNo());
            item.put("createdAt", doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : null);
            return item;
        }).toList();
        return Map.of("bidDocuments", items);
    }
}
