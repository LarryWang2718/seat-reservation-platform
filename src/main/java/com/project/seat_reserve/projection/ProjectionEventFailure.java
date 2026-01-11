package com.project.seat_reserve.projection;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    name = "projection_event_failure",
    uniqueConstraints = @UniqueConstraint(name = "uq_projection_event_failure_consumer_event", columnNames = {"consumer_name", "outbox_event_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectionEventFailure {
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
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @NotNull
    @Column(name = "last_error_message", nullable = false)
    private String lastErrorMessage;

    @NotNull
    @Column(name = "first_failed_at", nullable = false)
    private LocalDateTime firstFailedAt;

    @NotNull
    @Column(name = "last_failed_at", nullable = false)
    private LocalDateTime lastFailedAt;

    public static ProjectionEventFailure start(String consumerName, Long outboxEventId, String errorMessage, LocalDateTime failedAt) {
        ProjectionEventFailure failure = new ProjectionEventFailure();
        failure.setConsumerName(consumerName);
        failure.setOutboxEventId(outboxEventId);
        failure.setAttemptCount(1);
        failure.setLastErrorMessage(errorMessage);
        failure.setFirstFailedAt(failedAt);
        failure.setLastFailedAt(failedAt);
        return failure;
    }

    public void recordFailure(String errorMessage, LocalDateTime failedAt) {
        this.attemptCount = this.attemptCount + 1;
        this.lastErrorMessage = errorMessage;
        this.lastFailedAt = failedAt;
    }
}
