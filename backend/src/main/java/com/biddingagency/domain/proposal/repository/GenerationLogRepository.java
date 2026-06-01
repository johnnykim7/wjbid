package com.biddingagency.domain.proposal.repository;

import com.biddingagency.domain.proposal.entity.GenerationLog;
import com.biddingagency.domain.proposal.entity.GenerationTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GenerationLogRepository extends JpaRepository<GenerationLog, UUID> {

    List<GenerationLog> findByRunId(String runId);

    List<GenerationLog> findByTargetTypeAndTargetId(GenerationTargetType targetType, UUID targetId);
}
