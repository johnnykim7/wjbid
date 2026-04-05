package com.biddingagency.domain.event;

import com.biddingagency.domain.event.entity.EventLog;
import com.biddingagency.domain.event.repository.EventLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class EventLogListenerTest {

    @Mock
    private EventLogRepository eventLogRepository;

    @InjectMocks
    private EventLogListener listener;

    @Test
    @DisplayName("도메인이벤트수신_EventLog저장")
    void onDomainEvent_EventLog저장() {
        // given
        UUID entityId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        BidRequestCreatedEvent event = new BidRequestCreatedEvent(
                entityId, UUID.randomUUID(), UUID.randomUUID(), actorId);
        given(eventLogRepository.save(any(EventLog.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        listener.onDomainEvent(event);

        // then
        ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
        then(eventLogRepository).should().save(captor.capture());
        EventLog saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo("BidRequestCreated");
        assertThat(saved.getEntityType()).isEqualTo("BidRequest");
        assertThat(saved.getEntityId()).isEqualTo(entityId);
        assertThat(saved.getActorId()).isEqualTo(actorId);
        assertThat(saved.getPayload()).containsKey("occurredAt");
    }

    @Test
    @DisplayName("DeadlineApproachingEvent_EventLog저장")
    void onDeadlineEvent_EventLog저장() {
        // given
        UUID entityId = UUID.randomUUID();
        DeadlineApproachingEvent event = new DeadlineApproachingEvent(
                entityId, 3, java.time.LocalDateTime.now().plusDays(3));
        given(eventLogRepository.save(any(EventLog.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        listener.onDomainEvent(event);

        // then
        ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
        then(eventLogRepository).should().save(captor.capture());
        EventLog saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo("DeadlineApproaching");
        assertThat(saved.getEntityType()).isEqualTo("BidRequest");
        assertThat(saved.getEntityId()).isEqualTo(entityId);
    }
}
