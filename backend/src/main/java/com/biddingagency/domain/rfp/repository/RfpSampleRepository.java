package com.biddingagency.domain.rfp.repository;

import com.biddingagency.domain.rfp.entity.RfpSample;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RfpSampleRepository extends JpaRepository<RfpSample, UUID> {

    Page<RfpSample> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
