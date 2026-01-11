package com.project.seat_reserve.projection;

import java.time.LocalDateTime;

import com.project.seat_reserve.outbox.OutboxAggregateType;
import com.project.seat_reserve.outbox.OutboxEvent;
import com.project.seat_reserve.outbox.OutboxEventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "projection_dead_letter",
    uniqueConstraints = @UniqueConstraint(name = "uq_projection_dead_letter_consumer_event", columnNames = {"consumer_name", "outbox_event_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectionDeadLetter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "consumer_name", nullable = false)
    private String consumerName;

    @NotNull
    @Column(name = "outbox_event_id", nullable = false)
    private Long outboxEventId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "aggregate_type", nullable = false)
    private OutboxAggregateType aggregateType;

    @NotNull
    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private OutboxEventType eventType;

    @NotNull
    @Column(name = "payload", nullable = false)
    private String payload;

    @NotNull
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @NotNull
    @Column(name = "failure_reason", nullable = false)
    private String failureReason;

    @NotNull
    @Column(name = "first_failed_at", nullable = false)
    private LocalDateTime firstFailedAt;

    @NotNull
    @Column(name = "dead_lettered_at", nullable = false)
    private LocalDateTime deadLetteredAt;

    public static ProjectionDeadLetter fromFailure(
        String consumerName,
        OutboxEvent outboxEvent,
        ProjectionEventFailure failure,
        LocalDateTime deadLetteredAt
    ) {
        ProjectionDeadLetter deadLetter = new ProjectionDeadLetter();
        deadLetter.setConsumerName(consumerName);
        deadLetter.setOutboxEventId(outboxEvent.getId());
        deadLetter.setAggregateType(outboxEvent.getAggregateType());
        deadLetter.setAggregateId(outboxEvent.getAggregateId());
        deadLetter.setEventType(outboxEvent.getEventType());
        deadLetter.setPayload(outboxEvent.getPayload());
        deadLetter.setAttemptCount(failure.getAttemptCount());
        deadLetter.setFailureReason(failure.getLastErrorMessage());
        deadLetter.setFirstFailedAt(failure.getFirstFailedAt());
        deadLetter.setDeadLetteredAt(deadLetteredAt);
        return deadLetter;
    }
}
