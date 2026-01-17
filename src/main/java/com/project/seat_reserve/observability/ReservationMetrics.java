package com.project.seat_reserve.observability;

import java.util.Locale;

import org.springframework.stereotype.Component;

import com.project.seat_reserve.outbox.OutboxEventRepository;
import com.project.seat_reserve.projection.ProjectionCheckpointRepository;
import com.project.seat_reserve.projection.ProjectionConsumerService;
import com.project.seat_reserve.projection.ProjectionDeadLetterRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class ReservationMetrics {
    private static final String METRIC_PREFIX = "seat_reserve";

    private final MeterRegistry meterRegistry;
    private final OutboxEventRepository outboxEventRepository;
    private final ProjectionCheckpointRepository projectionCheckpointRepository;

    public ReservationMetrics(
        MeterRegistry meterRegistry,
        OutboxEventRepository outboxEventRepository,
        ProjectionCheckpointRepository projectionCheckpointRepository,
        ProjectionDeadLetterRepository projectionDeadLetterRepository
    ) {
        this.meterRegistry = meterRegistry;
        this.outboxEventRepository = outboxEventRepository;
        this.projectionCheckpointRepository = projectionCheckpointRepository;

        Gauge.builder(METRIC_PREFIX + ".projection.lag.events", this, ReservationMetrics::projectionLag)
            .description("Difference between latest outbox event id and projection checkpoint")
            .tag("consumer", ProjectionConsumerService.DEFAULT_CONSUMER_NAME)
            .register(meterRegistry);
        Gauge.builder(METRIC_PREFIX + ".projection.dead_letter.count", projectionDeadLetterRepository, repo -> repo.count())
            .description("Current number of projection dead-letter rows")
            .register(meterRegistry);
    }

    public void recordHoldAttempt() {
        counter("hold.attempts").increment();
    }

    public void recordHoldCreated() {
        counter("hold.created").increment();
    }

    public void recordHoldFailure(String reason) {
        counter("hold.failures", "reason", normalize(reason)).increment();
    }

    public void recordCheckoutAttempt() {
        counter("checkout.attempts").increment();
    }

    public void recordCheckoutCompleted(int ticketCount) {
        counter("checkout.completed").increment();
        counter("checkout.tickets.issued").increment(ticketCount);
    }

    public void recordCheckoutFailure(String reason) {
        counter("checkout.failures", "reason", normalize(reason)).increment();
    }

    public void recordProjectionEventsProcessed(int count) {
        if (count > 0) {
            counter("projection.events.processed").increment(count);
        }
    }

    public void recordProjectionRetry(String reason) {
        counter("projection.retries", "reason", normalize(reason)).increment();
    }

    public void recordProjectionDeadLetter(String reason) {
        counter("projection.dead_lettered", "reason", normalize(reason)).increment();
    }

    public void recordExpiredHolds(int count) {
        if (count > 0) {
            counter("hold.expired").increment(count);
        }
    }

    public void recordCancelledOrdersCleaned(int count) {
        if (count > 0) {
            counter("cleanup.cancelled_orders.cleaned").increment(count);
        }
    }

    private Counter counter(String suffix, String... tags) {
        return meterRegistry.counter(METRIC_PREFIX + "." + suffix, tags);
    }

    private double projectionLag() {
        long lastOutboxEventId = outboxEventRepository.findTopByOrderByIdDesc()
            .map(event -> event.getId())
            .orElse(0L);
        long lastProcessedEventId = projectionCheckpointRepository.findById(ProjectionConsumerService.DEFAULT_CONSUMER_NAME)
            .map(checkpoint -> checkpoint.getLastProcessedEventId())
            .orElse(0L);
        return Math.max(0L, lastOutboxEventId - lastProcessedEventId);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.replaceAll("[^A-Za-z0-9]+", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_|_$", "")
            .toLowerCase(Locale.ROOT);
    }
}
