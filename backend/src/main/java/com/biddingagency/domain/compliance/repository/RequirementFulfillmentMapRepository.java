package com.biddingagency.domain.compliance.repository;

import com.biddingagency.domain.compliance.entity.FulfillmentStatus;
import com.biddingagency.domain.compliance.entity.RequirementFulfillmentMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Requirement Fulfillment Map Repository
 */
@Repository
public interface RequirementFulfillmentMapRepository extends JpaRepository<RequirementFulfillmentMap, UUID> {

    /**
     * Find all mappings for a bid request
     */
    List<RequirementFulfillmentMap> findByBidRequestId(UUID bidRequestId);

    /**
     * Find by bid request and requirement
     */
    Optional<RequirementFulfillmentMap> findByBidRequestIdAndRequirementItemId(
            UUID bidRequestId, UUID requirementItemId);

    /**
     * Find by status
     */
    List<RequirementFulfillmentMap> findByBidRequestIdAndStatus(UUID bidRequestId, FulfillmentStatus status);

    /**
     * Count by status
     */
    long countByBidRequestIdAndStatus(UUID bidRequestId, FulfillmentStatus status);

    /**
     * Check if requirement is mapped
     */
    boolean existsByBidRequestIdAndRequirementItemId(UUID bidRequestId, UUID requirementItemId);

    /**
     * Get unfulfilled blocker requirements
     */
    @Query("SELECT m FROM RequirementFulfillmentMap m " +
            "JOIN FETCH m.requirementItem ri " +
            "WHERE m.bidRequest.id = :bidRequestId " +
            "AND ri.isBlocker = true " +
            "AND m.status = 'MISSING'")
    List<RequirementFulfillmentMap> findUnfulfilledBlockers(@Param("bidRequestId") UUID bidRequestId);

    /**
     * Delete all mappings for bid request
     */
    void deleteByBidRequestId(UUID bidRequestId);
}
