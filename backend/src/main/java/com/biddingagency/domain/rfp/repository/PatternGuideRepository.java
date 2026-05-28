package com.biddingagency.domain.rfp.repository;

import com.biddingagency.domain.rfp.entity.IndustryType;
import com.biddingagency.domain.rfp.entity.PatternGuide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatternGuideRepository extends JpaRepository<PatternGuide, UUID> {

    List<PatternGuide> findBySlotDefinitionId(UUID slotDefinitionId);

    Optional<PatternGuide> findBySlotDefinitionIdAndIndustryType(UUID slotDefinitionId, IndustryType industryType);

    /** 공통 가이드 (industryType=NULL). DB UNIQUE는 NULL을 중복으로 안 보므로 앱단에서 유일성 보장 */
    Optional<PatternGuide> findBySlotDefinitionIdAndIndustryTypeIsNull(UUID slotDefinitionId);
}
