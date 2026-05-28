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

    /** 공고유형별 가이드 1개 (UNIQUE) */
    Optional<PatternGuide> findByIndustryType(IndustryType industryType);

    List<PatternGuide> findAllByOrderByIndustryTypeAsc();
}
