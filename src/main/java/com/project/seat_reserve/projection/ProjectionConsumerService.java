package com.project.seat_reserve.projection;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.seat_reserve.observability.ReservationMetrics;
import com.project.seat_reserve.outbox.HoldCreatedPayload;
import com.project.seat_reserve.outbox.HoldExpiredPayload;
import com.project.seat_reserve.outbox.OrderCompletedPayload;
import com.project.seat_reserve.outbox.OutboxEvent;
import com.project.seat_reserve.outbox.OutboxEventRepository;
import com.project.seat_reserve.outbox.TicketIssuedPayload;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectionConsumerService {
    public static final String DEFAULT_CONSUMER_NAME = "seat-and-ticket-projection";
    private static final int DEFAULT_MAX_ATTEMPTS = 5;
    private static final int MAX_FAILURE_REASON_LENGTH = 1000;

    private final OutboxEventRepository outboxEventRepository;
    private final ProjectionCheckpointRepository projectionCheckpointRepository;
    private final ProjectionEventFailureRepository projectionEventFailureRepository;
    private final ProjectionDeadLetterRepository projectionDeadLetterRepository;
    private final SeatAvailabilityProjectionRepository seatAvailabilityProjectionRepository;
    private final UserTicketProjectionRepository userTicketProjectionRepository;
    private final ObjectMapper objectMapper;
    private final ReservationMetrics reservationMetrics;

    @Value("${app.projection-consumer.max-attempts:5}")
    private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

    @Transactional
    public int processNextBatch(String consumerName, int batchSize) {
        ProjectionCheckpoint checkpoint = projectionCheckpointRepository.findById(consumerName)
            .orElseGet(() -> ProjectionCheckpoint.initialize(consumerName, LocalDateTime.now()));

        List<OutboxEvent> outboxEvents = outboxEventRepository.findByIdGreaterThanOrderByIdAsc(
            checkpoint.getLastProcessedEventId(),
            PageRequest.of(0, batchSize)
        );

        if (outboxEvents.isEmpty()) {
            projectionCheckpointRepository.save(checkpoint);
            return 0;
        }

        int processedCount = 0;
        for (OutboxEvent outboxEvent : outboxEvents) {
            try {
                applyEvent(outboxEvent);
                projectionEventFailureRepository.deleteByConsumerNameAndOutboxEventId(consumerName, outboxEvent.getId());
                checkpoint.advanceTo(outboxEvent.getId(), LocalDateTime.now());
                processedCount++;
            } catch (RuntimeException exception) {
                if (recordFailureOrDeadLetter(consumerName, outboxEvent, exception)) {
                    checkpoint.advanceTo(outboxEvent.getId(), LocalDateTime.now());
                    processedCount++;
                    continue;
                }

                projectionCheckpointRepository.save(checkpoint);
                reservationMetrics.recordProjectionEventsProcessed(processedCount);
                if (processedCount > 0) {
                    log.info("Processed projection batch before retry stop: consumerName={}, processedCount={}, lastProcessedEventId={}",
                        consumerName, processedCount, checkpoint.getLastProcessedEventId());
                }
                return processedCount;
            }
        }

        projectionCheckpointRepository.save(checkpoint);
        reservationMetrics.recordProjectionEventsProcessed(processedCount);
        if (processedCount > 0) {
            log.info("Processed projection batch: consumerName={}, processedCount={}, lastProcessedEventId={}",
                consumerName, processedCount, checkpoint.getLastProcessedEventId());
        }
        return processedCount;
    }

    private boolean recordFailureOrDeadLetter(String consumerName, OutboxEvent outboxEvent, RuntimeException exception) {
        LocalDateTime failedAt = LocalDateTime.now();
        String failureReason = buildFailureReason(exception);
        ProjectionEventFailure failure = projectionEventFailureRepository.findByConsumerNameAndOutboxEventId(consumerName, outboxEvent.getId())
            .map(existing -> {
                existing.recordFailure(failureReason, failedAt);
                return existing;
            })
            .orElseGet(() -> ProjectionEventFailure.start(consumerName, outboxEvent.getId(), failureReason, failedAt));

        projectionEventFailureRepository.save(failure);
        if (failure.getAttemptCount() < maxAttempts) {
            reservationMetrics.recordProjectionRetry(failureReason);
            log.warn("Projection event processing failed: consumerName={}, outboxEventId={}, eventType={}, attemptCount={}, maxAttempts={}, reason={}",
                consumerName, outboxEvent.getId(), outboxEvent.getEventType(), failure.getAttemptCount(), maxAttempts, failureReason);
            return false;
        }

        projectionDeadLetterRepository.findByConsumerNameAndOutboxEventId(consumerName, outboxEvent.getId())
            .orElseGet(() -> projectionDeadLetterRepository.save(
                ProjectionDeadLetter.fromFailure(consumerName, outboxEvent, failure, failedAt)
            ));
        projectionEventFailureRepository.deleteByConsumerNameAndOutboxEventId(consumerName, outboxEvent.getId());
        reservationMetrics.recordProjectionDeadLetter(failureReason);
        log.error("Projection event dead-lettered: consumerName={}, outboxEventId={}, eventType={}, attemptCount={}, reason={}",
            consumerName, outboxEvent.getId(), outboxEvent.getEventType(), failure.getAttemptCount(), failureReason);
        return true;
    }

    private String buildFailureReason(RuntimeException exception) {
        String message = exception.getMessage();
        String failureReason = message == null || message.isBlank()
            ? exception.getClass().getSimpleName()
            : exception.getClass().getSimpleName() + ": " + message;
        if (failureReason.length() <= MAX_FAILURE_REASON_LENGTH) {
            return failureReason;
        }
        return failureReason.substring(0, MAX_FAILURE_REASON_LENGTH);
    }

    private void applyEvent(OutboxEvent outboxEvent) {
        switch (outboxEvent.getEventType()) {
            case HOLD_CREATED -> applyHoldCreated(outboxEvent);
            case HOLD_EXPIRED -> applyHoldExpired(outboxEvent);
            case ORDER_COMPLETED -> deserialize(outboxEvent.getPayload(), OrderCompletedPayload.class);
            case TICKET_ISSUED -> applyTicketIssued(outboxEvent);
        }
    }

    private void applyHoldCreated(OutboxEvent outboxEvent) {
        HoldCreatedPayload payload = deserialize(outboxEvent.getPayload(), HoldCreatedPayload.class);
        SeatAvailabilityProjection projection = seatAvailabilityProjectionRepository.findById(payload.seatId())
            .orElseThrow(() -> new IllegalStateException("Missing seat availability projection for seat " + payload.seatId()));
        projection.markHeld(payload.orderId(), payload.holdId(), payload.sessionId(), payload.expiresAt(), outboxEvent.getCreatedAt());
        seatAvailabilityProjectionRepository.save(projection);
    }

    private void applyHoldExpired(OutboxEvent outboxEvent) {
        HoldExpiredPayload payload = deserialize(outboxEvent.getPayload(), HoldExpiredPayload.class);
        SeatAvailabilityProjection projection = seatAvailabilityProjectionRepository.findById(payload.seatId())
            .orElseThrow(() -> new IllegalStateException("Missing seat availability projection for seat " + payload.seatId()));

        if (payload.holdId().equals(projection.getHoldId())) {
            projection.markAvailable(outboxEvent.getCreatedAt());
            seatAvailabilityProjectionRepository.save(projection);
        }
    }

    private void applyTicketIssued(OutboxEvent outboxEvent) {
        TicketIssuedPayload payload = deserialize(outboxEvent.getPayload(), TicketIssuedPayload.class);

        SeatAvailabilityProjection seatProjection = seatAvailabilityProjectionRepository.findById(payload.seatId())
            .orElseThrow(() -> new IllegalStateException("Missing seat availability projection for seat " + payload.seatId()));
        seatProjection.markSold(payload.orderId(), payload.ticketId(), payload.sessionId(), outboxEvent.getCreatedAt());
        seatAvailabilityProjectionRepository.save(seatProjection);

        UserTicketProjection ticketProjection = userTicketProjectionRepository.findById(payload.ticketId())
            .map(existing -> {
                existing.apply(payload, outboxEvent.getCreatedAt());
                return existing;
            })
            .orElseGet(() -> UserTicketProjection.fromPayload(payload, outboxEvent.getCreatedAt()));
        userTicketProjectionRepository.save(ticketProjection);
    }

    private <T> T deserialize(String payload, Class<T> payloadType) {
        try {
            return objectMapper.readValue(payload, payloadType);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to deserialize outbox payload", exception);
        }
    }
}
