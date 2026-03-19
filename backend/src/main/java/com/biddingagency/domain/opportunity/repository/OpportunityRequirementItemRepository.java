package com.biddingagency.domain.opportunity.repository;

import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import com.biddingagency.domain.opportunity.entity.RequirementCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Opportunity Requirement Item Repository
 */
@Repository
public interface OpportunityRequirementItemRepository extends JpaRepository<OpportunityRequirementItem, UUID> {

    /**
     * Find all requirements for opportunity
     */
    List<OpportunityRequirementItem> findByOpportunityId(UUID opportunityId);

    /**
     * Find by opportunity and category
     */
    List<OpportunityRequirementItem> findByOpportunityIdAndCategory(UUID opportunityId, RequirementCategory category);

    /**
     * Find blocker requirements
     */
    List<OpportunityRequirementItem> findByOpportunityIdAndIsBlocker(UUID opportunityId, Boolean isBlocker);

    /**
     * Count requirements for opportunity
     */
    long countByOpportunityId(UUID opportunityId);

    /**
     * Count blocker requirements
     */
    long countByOpportunityIdAndIsBlocker(UUID opportunityId, Boolean isBlocker);

    /**
     * Delete all requirements for opportunity
     */
    void deleteByOpportunityId(UUID opportunityId);
}
