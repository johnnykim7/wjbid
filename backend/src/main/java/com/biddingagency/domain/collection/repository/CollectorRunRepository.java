package com.biddingagency.domain.collection.repository;

import com.biddingagency.domain.collection.entity.CollectorRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CollectorRunRepository extends JpaRepository<CollectorRun, UUID> {

    Page<CollectorRun> findAllByOrderByStartedAtDesc(Pageable pageable);
}
