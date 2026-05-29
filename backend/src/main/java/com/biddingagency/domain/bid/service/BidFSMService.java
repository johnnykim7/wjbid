package com.biddingagency.domain.bid.service;

import com.biddingagency.domain.bid.CustomerTransitionNotAllowedException;
import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.compliance.RequirementSlotsNotFulfilledException;
import com.biddingagency.domain.compliance.service.ComplianceService;
import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.entity.DocumentType;
import com.biddingagency.domain.document.service.BidDocumentService;
import com.biddingagency.domain.event.BidRequestStateChangedEvent;
import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bid FSM Service
 *
 * Manages bid request state transitions with validation
 */
@Slf4j
@Service
public class BidFSMService {

    private final BidRequestRepository bidRequestRepository;
    private final AIWorkflowService aiWorkflowService;
    private final ApplicationEventPublisher eventPublisher;
    private final ComplianceService complianceService;
    private final BidDocumentService bidDocumentService;

    public BidFSMService(BidRequestRepository bidRequestRepository,
                         @Lazy AIWorkflowService aiWorkflowService,
                         ApplicationEventPublisher eventPublisher,
                         ComplianceService complianceService,
                         BidDocumentService bidDocumentService) {
        this.bidRequestRepository = bidRequestRepository;
        this.aiWorkflowService = aiWorkflowService;
        this.eventPublisher = eventPublisher;
        this.complianceService = complianceService;
        this.bidDocumentService = bidDocumentService;
    }

    // Valid state transitions map
    private static final Map<BidRequestState, List<BidRequestState>> VALID_TRANSITIONS = new HashMap<>();

    /**
     * CR-017 ③: 고객이 직접 호출할 수 있는 전이 화이트리스트.
     * 고객의 정당한 액션은 "서류 제출 완료"(DOCS_PENDING→DOCS_RECEIVED) 하나뿐.
     * 그 외(생성/검토/확정/제출)는 관리자 전용 — 관리자 검토 게이트를 코드로 강제.
     */
    private static final List<BidRequestState> CUSTOMER_ALLOWED_TARGET_STATES = List.of(
            BidRequestState.DOCS_RECEIVED
    );

    static {
        VALID_TRANSITIONS.put(BidRequestState.CREATED, List.of(
                BidRequestState.DOCS_PENDING,
                BidRequestState.CLOSED
        ));

        VALID_TRANSITIONS.put(BidRequestState.DOCS_PENDING, List.of(
                BidRequestState.DOCS_RECEIVED,
                BidRequestState.CLOSED
        ));

        VALID_TRANSITIONS.put(BidRequestState.DOCS_RECEIVED, List.of(
                BidRequestState.ANALYZING,
                BidRequestState.CLOSED
        ));

        VALID_TRANSITIONS.put(BidRequestState.ANALYZING, List.of(
                BidRequestState.GENERATING
        ));

        VALID_TRANSITIONS.put(BidRequestState.GENERATING, List.of(
                BidRequestState.REVIEW
        ));

        VALID_TRANSITIONS.put(BidRequestState.REVIEW, List.of(
                BidRequestState.CONFIRMED,
                BidRequestState.GENERATING  // 재생성 요청
        ));

        VALID_TRANSITIONS.put(BidRequestState.CONFIRMED, List.of(
                BidRequestState.SUBMITTED,
                BidRequestState.REVIEW  // 수정 필요
        ));

        // CR-018: 제출 후 입찰 결과 — 관리자가 합격/불합격 수동 업데이트
        VALID_TRANSITIONS.put(BidRequestState.SUBMITTED, List.of(
                BidRequestState.AWARDED,
                BidRequestState.NOT_AWARDED
        ));

        VALID_TRANSITIONS.put(BidRequestState.AWARDED, List.of());

        VALID_TRANSITIONS.put(BidRequestState.NOT_AWARDED, List.of());

        VALID_TRANSITIONS.put(BidRequestState.CLOSED, List.of());
    }

    /**
     * Check if transition is valid
     */
    public boolean canTransition(BidRequestState from, BidRequestState to) {
        List<BidRequestState> validTargets = VALID_TRANSITIONS.get(from);
        return validTargets != null && validTargets.contains(to);
    }

    /**
     * Get valid next states for current state
     */
    public List<BidRequestState> getValidNextStates(BidRequestState currentState) {
        return VALID_TRANSITIONS.getOrDefault(currentState, List.of());
    }

    /**
     * Transition bid request to new state
     *
     * @throws IllegalStateException if transition is invalid
     */
    @Transactional
    public BidRequest transition(UUID bidRequestId, BidRequestState toState,
                                   UUID userId, String username, String notes) {
        BidRequest bidRequest = bidRequestRepository.findById(bidRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Bid request not found: " + bidRequestId));

        BidRequestState currentState = bidRequest.getState();

        // Validate transition
        if (!canTransition(currentState, toState)) {
            String message = String.format(
                    "Invalid state transition from %s to %s for bid request %s",
                    currentState, toState, bidRequestId
            );
            log.error(message);
            throw new IllegalStateException(message);
        }

        // BIZ-015 (CR-010): DOCS_PENDING → DOCS_RECEIVED 전이는 BLOCKER 슬롯이 모두 충족되어야 함
        if (currentState == BidRequestState.DOCS_PENDING && toState == BidRequestState.DOCS_RECEIVED) {
            List<OpportunityRequirementItem> unfulfilled =
                    complianceService.getUnfulfilledBlockerSlots(bidRequestId);
            if (!unfulfilled.isEmpty()) {
                log.warn("Blocked DOCS_RECEIVED transition for {}: {} unfulfilled blocker slots",
                        bidRequestId, unfulfilled.size());
                throw new RequirementSlotsNotFulfilledException(unfulfilled);
            }
        }

        // Execute transition
        log.info("Transitioning bid request {} from {} to {} by user {}",
                bidRequestId, currentState, toState, username);

        bidRequest.transitionTo(toState, userId, username, notes);

        BidRequest saved = bidRequestRepository.save(bidRequest);

        log.info("Transition completed successfully");

        eventPublisher.publishEvent(new BidRequestStateChangedEvent(
                bidRequestId, currentState, toState, userId));

        // LLM 워크플로우 비동기 트리거 (상태별)
        triggerAIWorkflow(saved, toState);

        return saved;
    }

    /**
     * CR-017 ②: 개별 문서 타입 재생성 (REVIEW 상태에서 특정 문서만 다시 생성).
     * 전체 재생성(REVIEW→GENERATING 전이)과 달리 FSM 상태는 REVIEW 그대로 유지하고,
     * 결과는 새 버전으로 누적된다(Aimbase MCP save_document_version 콜백).
     *
     * @throws IllegalStateException REVIEW 상태가 아니거나, 대상 문서가 LOCKED인 경우
     */
    @Transactional
    public void regenerateDocument(UUID bidRequestId, DocumentType documentType) {
        BidRequest bidRequest = bidRequestRepository.findById(bidRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Bid request not found: " + bidRequestId));

        if (bidRequest.getState() != BidRequestState.REVIEW) {
            throw new IllegalStateException(
                    "개별 문서 재생성은 REVIEW 상태에서만 가능합니다. 현재 상태: " + bidRequest.getState());
        }

        // 대상 문서가 이미 있으면 LOCKED 검증 (없으면 신규 생성되므로 통과)
        try {
            BidDocument existing = bidDocumentService.findByBidRequestAndType(bidRequestId, documentType);
            if (existing.isLocked()) {
                throw new IllegalStateException("LOCKED 문서는 재생성할 수 없습니다: " + documentType);
            }
        } catch (IllegalArgumentException notFound) {
            // 해당 타입 문서 미존재 → 재생성이 곧 신규 생성. 허용.
        }

        log.info("개별 문서 재생성 요청: bidRequestId={}, documentType={}", bidRequestId, documentType);
        aiWorkflowService.regenerateSingleDocumentAsync(bidRequest, documentType);
    }

    /**
     * CR-017 ③: 고객이 호출하는 상태 전이 — 허용 화이트리스트 검증 후 transition 위임.
     * 고객은 CUSTOMER_ALLOWED_TARGET_STATES(=DOCS_RECEIVED)만 가능. 그 외는 관리자 전용이라 거부.
     *
     * @throws IllegalStateException 고객이 허용되지 않은 상태로 전이를 시도한 경우
     */
    @Transactional
    public BidRequest customerTransition(UUID bidRequestId, BidRequestState toState,
                                          UUID userId, String username, String notes) {
        if (!CUSTOMER_ALLOWED_TARGET_STATES.contains(toState)) {
            log.warn("고객 전이 거부: bidRequestId={}, toState={} (관리자 전용 전이)", bidRequestId, toState);
            throw new CustomerTransitionNotAllowedException(toState);
        }
        return transition(bidRequestId, toState, userId, username, notes);
    }

    /**
     * 상태 전환 후 LLM 워크플로우 비동기 실행
     * - REQUIREMENT_ANALYSIS 진입 → 요구사항 자동 추출
     * - DOCUMENT_DRAFTING 진입    → 문서 자동 생성
     */
    private void triggerAIWorkflow(BidRequest bidRequest, BidRequestState toState) {
        switch (toState) {
            case ANALYZING ->
                aiWorkflowService.extractRequirementsAsync(bidRequest);
            case GENERATING ->
                aiWorkflowService.generateDocumentsAsync(bidRequest);
            default -> { /* 다른 상태는 AI 작업 없음 */ }
        }
    }

    /**
     * Validate if bid request can move to READY_FOR_SUBMISSION
     * This is where BLOCKER validation will be integrated
     *
     * @throws IllegalStateException if blockers exist
     */
    public void validateReadyForSubmission(UUID bidRequestId) {
        // TODO: Integrate with ComplianceService to check for blockers
        // List<ComplianceIssue> blockers = complianceService.getBlockers(bidRequestId);
        // if (!blockers.isEmpty()) {
        //     throw new IllegalStateException("Cannot proceed: " + blockers.size() + " blocking issues found");
        // }

        log.info("Validation passed for bid request {}", bidRequestId);
    }

    /**
     * Batch get bid requests requiring client action
     */
    public List<BidRequest> getBidRequestsRequiringClientAction() {
        List<BidRequestState> clientActionStates = List.of(
                BidRequestState.DOCS_PENDING
        );
        return bidRequestRepository.findByStateIn(clientActionStates);
    }
}
