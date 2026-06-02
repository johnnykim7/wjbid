package com.biddingagency.domain.bid.service;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.dto.BidRequestDto;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.domain.event.BidRequestCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bid Request Service
 *
 * Handles bid request CRUD operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidRequestService {

    private final BidRequestRepository bidRequestRepository;
    private final MemberRepository memberRepository;
    private final OpportunityRepository opportunityRepository;
    private final BidFSMService fsmService;
    private final com.biddingagency.domain.notice.service.NoticeService noticeService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Create new bid request
     */
    @Transactional
    public BidRequest createBidRequest(UUID memberId, UUID opportunityId, UUID createdBy, String creatorName) {
        log.info("Creating bid request for member {} and opportunity {}", memberId, opportunityId);

        // Check if already exists
        if (bidRequestRepository.existsByMemberIdAndOpportunityId(memberId, opportunityId)) {
            throw new IllegalStateException("Bid request already exists for this member and opportunity");
        }

        // Load member and opportunity
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));

        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new IllegalArgumentException("Opportunity not found: " + opportunityId));

        // Check if opportunity is still active
        if (!opportunity.getActive()) {
            throw new IllegalStateException("Opportunity is not active");
        }

        // Check if deadline passed
        if (opportunity.isDeadlinePassed()) {
            throw new IllegalStateException("Opportunity deadline has passed");
        }

        // Create bid request
        BidRequest bidRequest = BidRequest.builder()
                .member(member)
                .opportunity(opportunity)
                .state(BidRequestState.CREATED)
                .build();

        // Initial state transition
        bidRequest.transitionTo(BidRequestState.CREATED, createdBy, creatorName, "Bid request created");

        BidRequest saved = bidRequestRepository.save(bidRequest);

        log.info("Bid request created: {}", saved.getId());

        eventPublisher.publishEvent(new BidRequestCreatedEvent(saved.getId(), memberId, opportunityId, createdBy));

        // 신청 즉시 문서 대기 상태로 전이 → 고객이 내 제안서에서 필요서류 업로드 + 제출 완료 가능
        // (CREATED → DOCS_PENDING 은 FSM 화이트리스트 허용 전이)
        BidRequest pending = fsmService.transition(
                saved.getId(), BidRequestState.DOCS_PENDING,
                createdBy, creatorName, "신청 접수 — 필요서류 제출 대기");

        return pending;
    }

    /**
     * Find by ID
     */
    public BidRequest findById(UUID id) {
        return bidRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bid request not found: " + id));
    }

    /**
     * Find by ID with details (eagerly loaded)
     */
    public BidRequest findByIdWithDetails(UUID id) {
        return bidRequestRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Bid request not found: " + id));
    }

    /**
     * Find by member
     */
    public Page<BidRequest> findByMember(UUID memberId, Pageable pageable) {
        return bidRequestRepository.findByMemberId(memberId, pageable);
    }

    /**
     * Find by state
     */
    public Page<BidRequest> findByState(BidRequestState state, Pageable pageable) {
        return bidRequestRepository.findByState(state, pageable);
    }

    /**
     * Find by assigned user
     */
    public Page<BidRequest> findByAssignedTo(UUID userId, Pageable pageable) {
        return bidRequestRepository.findByAssignedTo(userId, pageable);
    }

    // ── 관리자 목록/조회 — DTO를 트랜잭션 안에서 변환 (Entity 직렬화 금지, LazyInit 방지) ──

    /** 상태 필터 조회 → DTO. state==null이면 전체. */
    public Page<BidRequestDto> findAsDto(BidRequestState state, Pageable pageable) {
        Page<BidRequest> page = (state != null)
                ? bidRequestRepository.findByState(state, pageable)
                : bidRequestRepository.findAllWithDetails(pageable);
        return page.map(BidRequestDto::from);
    }

    /** 담당자 배정 목록 → DTO */
    public Page<BidRequestDto> findByAssignedToAsDto(UUID userId, Pageable pageable) {
        return bidRequestRepository.findByAssignedTo(userId, pageable).map(BidRequestDto::from);
    }

    /** 고객 액션 필요 목록 → DTO */
    public List<BidRequestDto> getRequestsRequiringClientActionAsDto() {
        return getRequestsRequiringClientAction().stream().map(BidRequestDto::from).toList();
    }

    /** 단건 → DTO (이력 포함). CR-024: 노출 노티 매핑(noticeId/displayTitle) 포함 */
    public BidRequestDto findByIdAsDto(UUID id) {
        BidRequest br = findByIdWithDetails(id);
        com.biddingagency.domain.notice.entity.Notice notice = br.getOpportunity() != null
                ? noticeService.findLatestVisibleByOpportunityId(br.getOpportunity().getId()).orElse(null)
                : null;
        return BidRequestDto.withHistory(br, notice);
    }

    /**
     * Assign to user
     */
    @Transactional
    public BidRequest assignTo(UUID bidRequestId, UUID userId) {
        BidRequest bidRequest = findById(bidRequestId);
        bidRequest.assignTo(userId);
        return bidRequestRepository.save(bidRequest);
    }

    /**
     * Count by state
     */
    public long countByState(BidRequestState state) {
        return bidRequestRepository.countByState(state);
    }

    /**
     * Get dashboard statistics
     */
    public Map<String, Long> getDashboardStats() {
        return Map.of(
                "CREATED", countByState(BidRequestState.CREATED),
                "DOCS_PENDING", countByState(BidRequestState.DOCS_PENDING),
                "DOCS_RECEIVED", countByState(BidRequestState.DOCS_RECEIVED),
                "ANALYZING", countByState(BidRequestState.ANALYZING),
                "GENERATING", countByState(BidRequestState.GENERATING),
                "REVIEW", countByState(BidRequestState.REVIEW),
                "CONFIRMED", countByState(BidRequestState.CONFIRMED),
                "SUBMITTED", countByState(BidRequestState.SUBMITTED),
                "CLOSED", countByState(BidRequestState.CLOSED)
        );
    }

    /**
     * Get bid requests requiring client action
     */
    public List<BidRequest> getRequestsRequiringClientAction() {
        return fsmService.getBidRequestsRequiringClientAction();
    }
}
