package com.biddingagency.domain.rfp.repository;

import com.biddingagency.domain.rfp.entity.IndustryType;
import com.biddingagency.domain.rfp.entity.RfpSample;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RfpSampleRepository extends JpaRepository<RfpSample, UUID> {

    Page<RfpSample> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** 패턴 추출/참조 대상 — 유형별 성공 제안서 (참조 허용된 것만) */
    List<RfpSample> findByIndustryTypeAndUseForPatternTrue(IndustryType industryType);
}
