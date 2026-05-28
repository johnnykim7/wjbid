package com.biddingagency.domain.rfp.repository;

import com.biddingagency.domain.rfp.entity.SlotAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SlotAssignmentRepository extends JpaRepository<SlotAssignment, UUID> {

    List<SlotAssignment> findByRfpSampleId(UUID rfpSampleId);

    /** 한 슬롯에 모인 전체 배치 (패턴 추출 입력) */
    List<SlotAssignment> findBySlotDefinitionId(UUID slotDefinitionId);
}
