package com.biddingagency.domain.event.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "event_logs",
        indexes = {
                @Index(name = "idx_event_logs_type", columnList = "event_type"),
                @Index(name = "idx_event_logs_entity", columnList = "entity_type, entity_id"),
                @Index(name = "idx_event_logs_created_at", columnList = "created_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EventLog extends BaseEntity {

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID entityId;

    @Column(name = "actor_id", columnDefinition = "BINARY(16)")
    private UUID actorId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "JSON")
    private Map<String, Object> payload;
}
