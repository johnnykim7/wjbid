package com.biddingagency.domain.opportunity.repository;

import com.biddingagency.domain.opportunity.entity.AnalysisStatus;
import com.biddingagency.domain.opportunity.entity.OpportunityAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OpportunityAnalysisRepository extends JpaRepository<OpportunityAnalysis, UUID> {

    Optional<OpportunityAnalysis> findByOpportunityId(UUID opportunityId);

    boolean existsByOpportunityId(UUID opportunityId);

    long countByStatus(AnalysisStatus status);
}
