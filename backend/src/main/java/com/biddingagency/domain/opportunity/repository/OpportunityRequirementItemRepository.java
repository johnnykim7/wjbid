package com.biddingagency.domain.opportunity.repository;

import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import com.biddingagency.domain.opportunity.entity.RequirementCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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
     * 공고 분석 순서대로 (CR-141)
     */
    List<OpportunityRequirementItem> findByOpportunityIdOrderBySortOrderAsc(UUID opportunityId);

    /**
     * 여러 공고의 요구 항목을 한 번에 (CR-141 제출 전 검사 목록)
     */
    @Query("SELECT ri FROM OpportunityRequirementItem ri WHERE ri.opportunity.id IN :opportunityIds ORDER BY ri.sortOrder")
    List<OpportunityRequirementItem> findByOpportunityIds(@Param("opportunityIds") Collection<UUID> opportunityIds);

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
