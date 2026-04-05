package com.biddingagency.domain.event;

import com.biddingagency.domain.event.entity.EventLog;
import com.biddingagency.domain.event.repository.EventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventLogListener {

    private final EventLogRepository eventLogRepository;

    @Async
    @EventListener
    public void onDomainEvent(DomainEvent event) {
        log.info("Domain event: type={}, entityId={}", event.getEventType(), event.getEntityId());

        EventLog eventLog = EventLog.builder()
                .eventType(event.getEventType())
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .actorId(event.getActorId())
                .payload(Map.of("occurredAt", event.getOccurredAt().toString()))
                .build();

        eventLogRepository.save(eventLog);
    }
}
