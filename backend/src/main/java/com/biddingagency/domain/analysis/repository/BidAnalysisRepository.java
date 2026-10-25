package com.biddingagency.domain.analysis.repository;

import com.biddingagency.domain.analysis.entity.BidAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BidAnalysisRepository extends JpaRepository<BidAnalysis, UUID> {

    /** 지금 분석: 가장 늦게 시작한 회차 */
    Optional<BidAnalysis> findFirstByBidRequestIdOrderByStartedAtDesc(UUID bidRequestId);

    boolean existsByBidRequestId(UUID bidRequestId);
}
