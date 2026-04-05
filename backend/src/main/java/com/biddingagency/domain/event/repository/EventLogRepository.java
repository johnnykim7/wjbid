package com.biddingagency.domain.event.repository;

import com.biddingagency.domain.event.entity.EventLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventLogRepository extends JpaRepository<EventLog, UUID> {

    List<EventLog> findByEntityTypeAndEntityId(String entityType, UUID entityId);

    Page<EventLog> findByEventType(String eventType, Pageable pageable);
}
