package com.biddingagency.domain.rfp.repository;

import com.biddingagency.domain.rfp.entity.RfpSampleFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RfpSampleFileRepository extends JpaRepository<RfpSampleFile, UUID> {

    List<RfpSampleFile> findByRfpSampleId(UUID rfpSampleId);
}
