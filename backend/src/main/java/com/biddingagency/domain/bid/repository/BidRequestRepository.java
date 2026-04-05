package com.biddingagency.domain.bid.repository;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Bid Request Repository
 */
@Repository
public interface BidRequestRepository extends JpaRepository<BidRequest, UUID> {

    /**
     * Find by member ID
     */
    Page<BidRequest> findByMemberId(UUID memberId, Pageable pageable);

    /**
     * Find by member ID and state
     */
    Page<BidRequest> findByMemberIdAndState(UUID memberId, BidRequestState state, Pageable pageable);

    /**
     * Find by state
     */
    Page<BidRequest> findByState(BidRequestState state, Pageable pageable);

    /**
     * Find by assigned to
     */
    Page<BidRequest> findByAssignedTo(UUID assignedTo, Pageable pageable);

    /**
     * Find by opportunity ID
     */
    List<BidRequest> findByOpportunityId(UUID opportunityId);

    /**
     * Check if member already has bid request for opportunity
     */
    boolean existsByMemberIdAndOpportunityId(UUID memberId, UUID opportunityId);

    /**
     * Find requests requiring client action
     */
    @Query("SELECT br FROM BidRequest br WHERE br.state IN :states")
    List<BidRequest> findByStateIn(@Param("states") List<BidRequestState> states);

    /**
     * Count by state
     */
    long countByState(BidRequestState state);

    /**
     * CR-003: Past submissions by member (for LLM 3-pipeline input)
     */
    List<BidRequest> findByMemberIdAndState(UUID memberId, BidRequestState state);

    /**
     * Find all with member and opportunity eagerly loaded
     */
    @Query("SELECT br FROM BidRequest br " +
            "JOIN FETCH br.member " +
            "JOIN FETCH br.opportunity " +
            "WHERE br.id = :id")
    Optional<BidRequest> findByIdWithDetails(@Param("id") UUID id);
}
