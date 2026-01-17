package com.project.seat_reserve.projection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.seat_reserve.observability.ReservationMetrics;
import com.project.seat_reserve.outbox.HoldCreatedPayload;
import com.project.seat_reserve.outbox.HoldExpiredPayload;
import com.project.seat_reserve.outbox.OrderCompletedPayload;
import com.project.seat_reserve.outbox.OutboxAggregateType;
import com.project.seat_reserve.outbox.OutboxEvent;
import com.project.seat_reserve.outbox.OutboxEventRepository;
import com.project.seat_reserve.outbox.OutboxEventType;
import com.project.seat_reserve.outbox.TicketIssuedPayload;

@ExtendWith(MockitoExtension.class)
class ProjectionConsumerServiceTest {
    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ProjectionCheckpointRepository projectionCheckpointRepository;

    @Mock
    private ProjectionEventFailureRepository projectionEventFailureRepository;

    @Mock
    private ProjectionDeadLetterRepository projectionDeadLetterRepository;

    @Mock
    private SeatAvailabilityProjectionRepository seatAvailabilityProjectionRepository;

    @Mock
    private UserTicketProjectionRepository userTicketProjectionRepository;

    @Mock
    private ReservationMetrics reservationMetrics;

    @Captor
    private ArgumentCaptor<ProjectionCheckpoint> checkpointCaptor;

    @Captor
    private ArgumentCaptor<UserTicketProjection> userTicketCaptor;

    @Captor
    private ArgumentCaptor<ProjectionEventFailure> failureCaptor;

    @Captor
    private ArgumentCaptor<ProjectionDeadLetter> deadLetterCaptor;

    private ProjectionConsumerService projectionConsumerService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        projectionConsumerService = new ProjectionConsumerService(
            outboxEventRepository,
            projectionCheckpointRepository,
            projectionEventFailureRepository,
            projectionDeadLetterRepository,
            seatAvailabilityProjectionRepository,
            userTicketProjectionRepository,
            objectMapper,
            reservationMetrics
        );
        ReflectionTestUtils.setField(projectionConsumerService, "maxAttempts", 3);
    }

    @Test
    void processNextBatchProjectsEventsAndAdvancesCheckpoint() throws Exception {
        String consumerName = ProjectionConsumerService.DEFAULT_CONSUMER_NAME;
        SeatAvailabilityProjection seatProjection = new SeatAvailabilityProjection(
            10L, 20L, "A", "1", "10", SeatAvailabilityStatus.AVAILABLE, null, null, null, null, null, LocalDateTime.now()
        );

        OutboxEvent holdCreated = createHoldCreatedEvent(1L, 100L, 200L, 20L, 10L, "session-1",
            LocalDateTime.of(2026, 3, 17, 10, 0), LocalDateTime.of(2026, 3, 17, 10, 5));
        OutboxEvent orderCompleted = createOrderCompletedEvent(2L, 200L, 20L, "session-1", List.of(100L), List.of(300L),
            LocalDateTime.of(2026, 3, 17, 10, 1));
        OutboxEvent ticketIssued = createTicketIssuedEvent(3L, 300L, 200L, 20L, "session-1", 10L, "A", "1", "10",
            LocalDateTime.of(2026, 3, 17, 10, 1));

        when(projectionCheckpointRepository.findById(consumerName)).thenReturn(Optional.empty());
        when(outboxEventRepository.findByIdGreaterThanOrderByIdAsc(any(Long.class), any(Pageable.class)))
            .thenReturn(List.of(holdCreated, orderCompleted, ticketIssued));
        when(seatAvailabilityProjectionRepository.findById(10L)).thenReturn(Optional.of(seatProjection));
        when(seatAvailabilityProjectionRepository.save(any(SeatAvailabilityProjection.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userTicketProjectionRepository.findById(300L)).thenReturn(Optional.empty());
        when(userTicketProjectionRepository.save(userTicketCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        int processed = projectionConsumerService.processNextBatch(consumerName, 10);

        assertEquals(3, processed);
        assertEquals(SeatAvailabilityStatus.SOLD, seatProjection.getStatus());
        assertEquals(300L, seatProjection.getTicketId());
        assertEquals("session-1", seatProjection.getSessionId());

        UserTicketProjection userTicketProjection = userTicketCaptor.getValue();
        assertEquals(300L, userTicketProjection.getTicketId());
        assertEquals("session-1", userTicketProjection.getSessionId());
        assertEquals(10L, userTicketProjection.getSeatId());

        verify(projectionEventFailureRepository, times(3)).deleteByConsumerNameAndOutboxEventId(any(String.class), any(Long.class));
        verify(projectionCheckpointRepository).save(checkpointCaptor.capture());
        assertEquals(3L, checkpointCaptor.getValue().getLastProcessedEventId());
    }

    @Test
    void processNextBatchProjectsHoldExpiredAndClearsSeatAvailability() throws Exception {
        String consumerName = ProjectionConsumerService.DEFAULT_CONSUMER_NAME;
        SeatAvailabilityProjection seatProjection = new SeatAvailabilityProjection(
            10L, 20L, "A", "1", "10", SeatAvailabilityStatus.HELD, 200L, 100L, "session-1",
            LocalDateTime.of(2026, 3, 17, 10, 5), null, LocalDateTime.now()
        );
        OutboxEvent holdExpired = createHoldExpiredEvent(1L, 100L, 200L, 20L, 10L, "session-1", LocalDateTime.of(2026, 3, 17, 10, 6));

        when(projectionCheckpointRepository.findById(consumerName)).thenReturn(Optional.of(ProjectionCheckpoint.initialize(consumerName, LocalDateTime.now())));
        when(outboxEventRepository.findByIdGreaterThanOrderByIdAsc(any(Long.class), any(Pageable.class)))
            .thenReturn(List.of(holdExpired));
        when(seatAvailabilityProjectionRepository.findById(10L)).thenReturn(Optional.of(seatProjection));
        when(seatAvailabilityProjectionRepository.save(any(SeatAvailabilityProjection.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int processed = projectionConsumerService.processNextBatch(consumerName, 10);

        assertEquals(1, processed);
        assertEquals(SeatAvailabilityStatus.AVAILABLE, seatProjection.getStatus());
        assertNull(seatProjection.getOrderId());
        assertNull(seatProjection.getHoldId());
        assertNull(seatProjection.getSessionId());
        assertNull(seatProjection.getHoldExpiresAt());
        assertNull(seatProjection.getTicketId());
        verify(projectionCheckpointRepository).save(checkpointCaptor.capture());
        assertEquals(1L, checkpointCaptor.getValue().getLastProcessedEventId());
    }

    @Test
    void processNextBatchIgnoresStaleHoldExpiredWhenProjectionAlreadyPointsAtDifferentHold() throws Exception {
        String consumerName = ProjectionConsumerService.DEFAULT_CONSUMER_NAME;
        SeatAvailabilityProjection seatProjection = new SeatAvailabilityProjection(
            10L, 20L, "A", "1", "10", SeatAvailabilityStatus.HELD, 201L, 101L, "session-2",
            LocalDateTime.of(2026, 3, 17, 10, 8), null, LocalDateTime.now()
        );
        OutboxEvent holdExpired = createHoldExpiredEvent(1L, 100L, 200L, 20L, 10L, "session-1", LocalDateTime.of(2026, 3, 17, 10, 6));

        when(projectionCheckpointRepository.findById(consumerName)).thenReturn(Optional.of(ProjectionCheckpoint.initialize(consumerName, LocalDateTime.now())));
        when(outboxEventRepository.findByIdGreaterThanOrderByIdAsc(any(Long.class), any(Pageable.class)))
            .thenReturn(List.of(holdExpired));
        when(seatAvailabilityProjectionRepository.findById(10L)).thenReturn(Optional.of(seatProjection));

        int processed = projectionConsumerService.processNextBatch(consumerName, 10);

        assertEquals(1, processed);
        assertEquals(SeatAvailabilityStatus.HELD, seatProjection.getStatus());
        assertEquals(201L, seatProjection.getOrderId());
        assertEquals(101L, seatProjection.getHoldId());
        assertEquals("session-2", seatProjection.getSessionId());
        verify(seatAvailabilityProjectionRepository, never()).save(any(SeatAvailabilityProjection.class));
        verify(projectionCheckpointRepository).save(checkpointCaptor.capture());
        assertEquals(1L, checkpointCaptor.getValue().getLastProcessedEventId());
    }

    @Test
    void replayRebuildsSeatAvailabilityProjectionFromOutboxLogAcrossBatches() throws Exception {
        String consumerName = ProjectionConsumerService.DEFAULT_CONSUMER_NAME;
        SeatAvailabilityProjection seatProjection = new SeatAvailabilityProjection(
            10L, 20L, "A", "1", "10", SeatAvailabilityStatus.AVAILABLE, null, null, null, null, null, LocalDateTime.now()
        );
        AtomicReference<ProjectionCheckpoint> checkpointState = new AtomicReference<>();

        List<OutboxEvent> outboxEvents = List.of(
            createHoldCreatedEvent(1L, 100L, 200L, 20L, 10L, "session-1", LocalDateTime.of(2026, 3, 17, 10, 0), LocalDateTime.of(2026, 3, 17, 10, 5)),
            createOrderCompletedEvent(2L, 200L, 20L, "session-1", List.of(100L), List.of(300L), LocalDateTime.of(2026, 3, 17, 10, 1)),
            createTicketIssuedEvent(3L, 300L, 200L, 20L, "session-1", 10L, "A", "1", "10", LocalDateTime.of(2026, 3, 17, 10, 1))
        );

        when(projectionCheckpointRepository.findById(consumerName)).thenAnswer(invocation -> Optional.ofNullable(checkpointState.get()));
        when(projectionCheckpointRepository.save(any(ProjectionCheckpoint.class))).thenAnswer(invocation -> {
            ProjectionCheckpoint checkpoint = invocation.getArgument(0);
            checkpointState.set(new ProjectionCheckpoint(
                checkpoint.getConsumerName(),
                checkpoint.getLastProcessedEventId(),
                checkpoint.getUpdatedAt()
            ));
            return checkpoint;
        });
        when(outboxEventRepository.findByIdGreaterThanOrderByIdAsc(any(Long.class), any(Pageable.class))).thenAnswer(invocation -> {
            Long lastProcessedId = invocation.getArgument(0);
            Pageable pageable = invocation.getArgument(1);
            return outboxEvents.stream()
                .filter(event -> event.getId() > lastProcessedId)
                .limit(pageable.getPageSize())
                .toList();
        });
        when(seatAvailabilityProjectionRepository.findById(10L)).thenReturn(Optional.of(seatProjection));
        when(seatAvailabilityProjectionRepository.save(any(SeatAvailabilityProjection.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userTicketProjectionRepository.findById(300L)).thenReturn(Optional.empty());
        when(userTicketProjectionRepository.save(any(UserTicketProjection.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(2, projectionConsumerService.processNextBatch(consumerName, 2));
        assertEquals(SeatAvailabilityStatus.HELD, seatProjection.getStatus());
        assertEquals(2L, checkpointState.get().getLastProcessedEventId());

        assertEquals(1, projectionConsumerService.processNextBatch(consumerName, 2));
        assertEquals(SeatAvailabilityStatus.SOLD, seatProjection.getStatus());
        assertEquals(300L, seatProjection.getTicketId());
        assertEquals("session-1", seatProjection.getSessionId());
        assertEquals(3L, checkpointState.get().getLastProcessedEventId());

        assertEquals(0, projectionConsumerService.processNextBatch(consumerName, 2));
        assertEquals(3L, checkpointState.get().getLastProcessedEventId());
    }

    @Test
    void replayRebuildsUserTicketProjectionFromOutboxLog() throws Exception {
        String consumerName = ProjectionConsumerService.DEFAULT_CONSUMER_NAME;
        SeatAvailabilityProjection seatProjection = new SeatAvailabilityProjection(
            10L, 20L, "A", "1", "10", SeatAvailabilityStatus.AVAILABLE, null, null, null, null, null, LocalDateTime.now()
        );
        AtomicReference<ProjectionCheckpoint> checkpointState = new AtomicReference<>();
        Map<Long, UserTicketProjection> ticketProjectionStore = new HashMap<>();

        List<OutboxEvent> outboxEvents = List.of(
            createHoldCreatedEvent(1L, 100L, 200L, 20L, 10L, "session-1", LocalDateTime.of(2026, 3, 17, 10, 0), LocalDateTime.of(2026, 3, 17, 10, 5)),
            createOrderCompletedEvent(2L, 200L, 20L, "session-1", List.of(100L), List.of(300L), LocalDateTime.of(2026, 3, 17, 10, 1)),
            createTicketIssuedEvent(3L, 300L, 200L, 20L, "session-1", 10L, "A", "1", "10", LocalDateTime.of(2026, 3, 17, 10, 1))
        );

        when(projectionCheckpointRepository.findById(consumerName)).thenAnswer(invocation -> Optional.ofNullable(checkpointState.get()));
        when(projectionCheckpointRepository.save(any(ProjectionCheckpoint.class))).thenAnswer(invocation -> {
            ProjectionCheckpoint checkpoint = invocation.getArgument(0);
            checkpointState.set(new ProjectionCheckpoint(
                checkpoint.getConsumerName(),
                checkpoint.getLastProcessedEventId(),
                checkpoint.getUpdatedAt()
            ));
            return checkpoint;
        });
        when(outboxEventRepository.findByIdGreaterThanOrderByIdAsc(any(Long.class), any(Pageable.class))).thenAnswer(invocation -> {
            Long lastProcessedId = invocation.getArgument(0);
            Pageable pageable = invocation.getArgument(1);
            return outboxEvents.stream()
                .filter(event -> event.getId() > lastProcessedId)
                .limit(pageable.getPageSize())
                .toList();
        });
        when(seatAvailabilityProjectionRepository.findById(10L)).thenReturn(Optional.of(seatProjection));
        when(seatAvailabilityProjectionRepository.save(any(SeatAvailabilityProjection.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userTicketProjectionRepository.findById(any(Long.class))).thenAnswer(invocation -> Optional.ofNullable(ticketProjectionStore.get(invocation.getArgument(0))));
        when(userTicketProjectionRepository.save(any(UserTicketProjection.class))).thenAnswer(invocation -> {
            UserTicketProjection projection = invocation.getArgument(0);
            ticketProjectionStore.put(projection.getTicketId(), projection);
            return projection;
        });

        assertEquals(3, projectionConsumerService.processNextBatch(consumerName, 10));

        UserTicketProjection rebuiltProjection = ticketProjectionStore.get(300L);
        assertNotNull(rebuiltProjection);
        assertEquals(300L, rebuiltProjection.getTicketId());
        assertEquals(200L, rebuiltProjection.getOrderId());
        assertEquals(20L, rebuiltProjection.getEventId());
        assertEquals("session-1", rebuiltProjection.getSessionId());
        assertEquals(10L, rebuiltProjection.getSeatId());
        assertEquals("10", rebuiltProjection.getSeatNumber());
        assertEquals(3L, checkpointState.get().getLastProcessedEventId());
    }

    @Test
    void processNextBatchRecordsRetryableFailureAndLeavesCheckpointBehind() throws Exception {
        String consumerName = ProjectionConsumerService.DEFAULT_CONSUMER_NAME;
        SeatAvailabilityProjection seatProjection = new SeatAvailabilityProjection(
            10L, 20L, "A", "1", "10", SeatAvailabilityStatus.HELD, 200L, 100L, "session-1",
            LocalDateTime.of(2026, 3, 17, 10, 5), null, LocalDateTime.now()
        );
        OutboxEvent ticketIssued = createTicketIssuedEvent(1L, 300L, 200L, 20L, "session-1", 10L, "A", "1", "10",
            LocalDateTime.of(2026, 3, 17, 10, 1));

        when(projectionCheckpointRepository.findById(consumerName)).thenReturn(Optional.of(ProjectionCheckpoint.initialize(consumerName, LocalDateTime.now())));
        when(outboxEventRepository.findByIdGreaterThanOrderByIdAsc(any(Long.class), any(Pageable.class)))
            .thenReturn(List.of(ticketIssued));
        when(seatAvailabilityProjectionRepository.findById(10L)).thenReturn(Optional.of(seatProjection));
        when(seatAvailabilityProjectionRepository.save(any(SeatAvailabilityProjection.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userTicketProjectionRepository.findById(300L)).thenReturn(Optional.empty());
        when(userTicketProjectionRepository.save(any(UserTicketProjection.class))).thenThrow(new IllegalStateException("projection write failed"));
        when(projectionEventFailureRepository.findByConsumerNameAndOutboxEventId(consumerName, 1L)).thenReturn(Optional.empty());
        when(projectionEventFailureRepository.save(failureCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        int processed = projectionConsumerService.processNextBatch(consumerName, 10);

        assertEquals(0, processed);
        verify(projectionDeadLetterRepository, never()).save(any(ProjectionDeadLetter.class));
        verify(projectionCheckpointRepository).save(checkpointCaptor.capture());
        assertEquals(0L, checkpointCaptor.getValue().getLastProcessedEventId());
        assertEquals(1, failureCaptor.getValue().getAttemptCount());
        assertEquals(consumerName, failureCaptor.getValue().getConsumerName());
        assertEquals(1L, failureCaptor.getValue().getOutboxEventId());
    }

    @Test
    void processNextBatchDeadLettersPoisonEventAfterMaxAttemptsAndAdvancesCheckpoint() throws Exception {
        String consumerName = ProjectionConsumerService.DEFAULT_CONSUMER_NAME;
        SeatAvailabilityProjection seatProjection = new SeatAvailabilityProjection(
            10L, 20L, "A", "1", "10", SeatAvailabilityStatus.HELD, 200L, 100L, "session-1",
            LocalDateTime.of(2026, 3, 17, 10, 5), null, LocalDateTime.now()
        );
        OutboxEvent ticketIssued = createTicketIssuedEvent(1L, 300L, 200L, 20L, "session-1", 10L, "A", "1", "10",
            LocalDateTime.of(2026, 3, 17, 10, 1));
        ProjectionEventFailure existingFailure = ProjectionEventFailure.start(consumerName, 1L, "IllegalStateException: prior failure", LocalDateTime.of(2026, 3, 17, 10, 2));
        existingFailure.recordFailure("IllegalStateException: prior failure", LocalDateTime.of(2026, 3, 17, 10, 3));

        when(projectionCheckpointRepository.findById(consumerName)).thenReturn(Optional.of(ProjectionCheckpoint.initialize(consumerName, LocalDateTime.now())));
        when(outboxEventRepository.findByIdGreaterThanOrderByIdAsc(any(Long.class), any(Pageable.class)))
            .thenReturn(List.of(ticketIssued));
        when(seatAvailabilityProjectionRepository.findById(10L)).thenReturn(Optional.of(seatProjection));
        when(seatAvailabilityProjectionRepository.save(any(SeatAvailabilityProjection.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userTicketProjectionRepository.findById(300L)).thenReturn(Optional.empty());
        when(userTicketProjectionRepository.save(any(UserTicketProjection.class))).thenThrow(new IllegalStateException("projection write failed"));
        when(projectionEventFailureRepository.findByConsumerNameAndOutboxEventId(consumerName, 1L)).thenReturn(Optional.of(existingFailure));
        when(projectionEventFailureRepository.save(failureCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        when(projectionDeadLetterRepository.findByConsumerNameAndOutboxEventId(consumerName, 1L)).thenReturn(Optional.empty());
        when(projectionDeadLetterRepository.save(deadLetterCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        int processed = projectionConsumerService.processNextBatch(consumerName, 10);

        assertEquals(1, processed);
        verify(projectionCheckpointRepository).save(checkpointCaptor.capture());
        assertEquals(1L, checkpointCaptor.getValue().getLastProcessedEventId());
        assertEquals(3, failureCaptor.getValue().getAttemptCount());
        assertEquals(1L, deadLetterCaptor.getValue().getOutboxEventId());
        assertEquals(3, deadLetterCaptor.getValue().getAttemptCount());
        verify(projectionEventFailureRepository).deleteByConsumerNameAndOutboxEventId(consumerName, 1L);
    }

    private OutboxEvent createHoldCreatedEvent(
        Long eventId,
        Long holdId,
        Long orderId,
        Long eventAggregateId,
        Long seatId,
        String sessionId,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
    ) throws Exception {
        OutboxEvent holdCreated = OutboxEvent.create(
            OutboxAggregateType.HOLD,
            holdId,
            OutboxEventType.HOLD_CREATED,
            objectMapper.writeValueAsString(new HoldCreatedPayload(holdId, orderId, eventAggregateId, seatId, sessionId, createdAt, expiresAt)),
            createdAt
        );
        holdCreated.setId(eventId);
        return holdCreated;
    }

    private OutboxEvent createHoldExpiredEvent(
        Long eventId,
        Long holdId,
        Long orderId,
        Long eventAggregateId,
        Long seatId,
        String sessionId,
        LocalDateTime expiredAt
    ) throws Exception {
        OutboxEvent holdExpired = OutboxEvent.create(
            OutboxAggregateType.HOLD,
            holdId,
            OutboxEventType.HOLD_EXPIRED,
            objectMapper.writeValueAsString(new HoldExpiredPayload(holdId, orderId, eventAggregateId, seatId, sessionId, expiredAt)),
            expiredAt
        );
        holdExpired.setId(eventId);
        return holdExpired;
    }

    private OutboxEvent createOrderCompletedEvent(
        Long eventId,
        Long orderId,
        Long eventAggregateId,
        String sessionId,
        List<Long> holdIds,
        List<Long> ticketIds,
        LocalDateTime completedAt
    ) throws Exception {
        OutboxEvent orderCompleted = OutboxEvent.create(
            OutboxAggregateType.ORDER,
            orderId,
            OutboxEventType.ORDER_COMPLETED,
            objectMapper.writeValueAsString(new OrderCompletedPayload(orderId, eventAggregateId, sessionId, holdIds, ticketIds, completedAt)),
            completedAt
        );
        orderCompleted.setId(eventId);
        return orderCompleted;
    }

    private OutboxEvent createTicketIssuedEvent(
        Long eventId,
        Long ticketId,
        Long orderId,
        Long eventAggregateId,
        String sessionId,
        Long seatId,
        String section,
        String rowLabel,
        String seatNumber,
        LocalDateTime issuedAt
    ) throws Exception {
        OutboxEvent ticketIssued = OutboxEvent.create(
            OutboxAggregateType.TICKET,
            ticketId,
            OutboxEventType.TICKET_ISSUED,
            objectMapper.writeValueAsString(new TicketIssuedPayload(ticketId, orderId, eventAggregateId, sessionId, seatId, section, rowLabel, seatNumber, issuedAt)),
            issuedAt
        );
        ticketIssued.setId(eventId);
        return ticketIssued;
    }
}
