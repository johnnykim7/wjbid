package com.biddingagency.domain.federal.repository;

import com.biddingagency.domain.federal.entity.FederalContractSyncRun;
import com.biddingagency.domain.federal.entity.FederalSyncStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FederalContractSyncRunRepository extends JpaRepository<FederalContractSyncRun, UUID> {

    List<FederalContractSyncRun> findByStatus(FederalSyncStatus status);

    boolean existsByStatus(FederalSyncStatus status);

    Optional<FederalContractSyncRun> findFirstByStatusOrderByStartedAtDesc(FederalSyncStatus status);

    List<FederalContractSyncRun> findAllByOrderByStartedAtDesc(Pageable pageable);
}
