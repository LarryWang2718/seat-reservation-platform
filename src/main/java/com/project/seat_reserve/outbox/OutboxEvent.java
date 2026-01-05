package com.project.seat_reserve.outbox;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "outbox_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Aggregate type cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "aggregate_type", nullable = false)
    private OutboxAggregateType aggregateType;

    @NotNull(message = "Aggregate ID cannot be null")
    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @NotNull(message = "Event type cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private OutboxEventType eventType;

    @NotNull(message = "Payload cannot be null")
    @Column(name = "payload", nullable = false)
    private String payload;

    @NotNull(message = "Created at cannot be null")
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static OutboxEvent create(
        OutboxAggregateType aggregateType,
        Long aggregateId,
        OutboxEventType eventType,
        String payload,
        LocalDateTime createdAt
    ) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setAggregateType(aggregateType);
        outboxEvent.setAggregateId(aggregateId);
        outboxEvent.setEventType(eventType);
        outboxEvent.setPayload(payload);
        outboxEvent.setCreatedAt(createdAt);
        return outboxEvent;
    }
}
