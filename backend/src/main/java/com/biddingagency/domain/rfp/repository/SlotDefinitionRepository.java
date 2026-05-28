package com.biddingagency.domain.rfp.repository;

import com.biddingagency.domain.rfp.entity.SlotDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SlotDefinitionRepository extends JpaRepository<SlotDefinition, UUID> {

    List<SlotDefinition> findAllByOrderByDisplayOrderAsc();

    Optional<SlotDefinition> findBySlotCode(String slotCode);
}
