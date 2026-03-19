package com.biddingagency.domain.bid.service;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        return saved;
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
