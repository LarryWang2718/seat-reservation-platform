package com.project.seat_reserve.projection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.seat_reserve.outbox.HoldCreatedPayload;
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
    private SeatAvailabilityProjectionRepository seatAvailabilityProjectionRepository;

    @Mock
    private UserTicketProjectionRepository userTicketProjectionRepository;

    @Captor
    private ArgumentCaptor<ProjectionCheckpoint> checkpointCaptor;

    @Captor
    private ArgumentCaptor<UserTicketProjection> userTicketCaptor;

    private ProjectionConsumerService projectionConsumerService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        projectionConsumerService = new ProjectionConsumerService(
            outboxEventRepository,
            projectionCheckpointRepository,
            seatAvailabilityProjectionRepository,
            userTicketProjectionRepository,
            objectMapper
        );
    }

    @Test
    void processNextBatchProjectsEventsAndAdvancesCheckpoint() throws Exception {
        String consumerName = ProjectionConsumerService.DEFAULT_CONSUMER_NAME;
        SeatAvailabilityProjection seatProjection = new SeatAvailabilityProjection(
            10L, 20L, "A", "1", "10", SeatAvailabilityStatus.AVAILABLE, null, null, null, null, null, LocalDateTime.now()
        );

        OutboxEvent holdCreated = OutboxEvent.create(
            OutboxAggregateType.HOLD,
            100L,
            OutboxEventType.HOLD_CREATED,
            objectMapper.writeValueAsString(new HoldCreatedPayload(100L, 200L, 20L, 10L, "session-1", LocalDateTime.of(2026, 3, 17, 10, 0), LocalDateTime.of(2026, 3, 17, 10, 5))),
            LocalDateTime.of(2026, 3, 17, 10, 0)
        );
        holdCreated.setId(1L);

        OutboxEvent orderCompleted = OutboxEvent.create(
            OutboxAggregateType.ORDER,
            200L,
            OutboxEventType.ORDER_COMPLETED,
            objectMapper.writeValueAsString(new OrderCompletedPayload(200L, 20L, "session-1", List.of(100L), List.of(300L), LocalDateTime.of(2026, 3, 17, 10, 1))),
            LocalDateTime.of(2026, 3, 17, 10, 1)
        );
        orderCompleted.setId(2L);

        OutboxEvent ticketIssued = OutboxEvent.create(
            OutboxAggregateType.TICKET,
            300L,
            OutboxEventType.TICKET_ISSUED,
            objectMapper.writeValueAsString(new TicketIssuedPayload(300L, 200L, 20L, "session-1", 10L, "A", "1", "10", LocalDateTime.of(2026, 3, 17, 10, 1))),
            LocalDateTime.of(2026, 3, 17, 10, 1)
        );
        ticketIssued.setId(3L);

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

        verify(projectionCheckpointRepository).save(checkpointCaptor.capture());
        assertEquals(3L, checkpointCaptor.getValue().getLastProcessedEventId());
    }

    @Test
    void processNextBatchDoesNotAdvanceCheckpointWhenProjectionFails() throws Exception {
        String consumerName = ProjectionConsumerService.DEFAULT_CONSUMER_NAME;
        SeatAvailabilityProjection seatProjection = new SeatAvailabilityProjection(
            10L, 20L, "A", "1", "10", SeatAvailabilityStatus.HELD, 200L, 100L, "session-1", LocalDateTime.of(2026, 3, 17, 10, 5), null, LocalDateTime.now()
        );

        OutboxEvent ticketIssued = OutboxEvent.create(
            OutboxAggregateType.TICKET,
            300L,
            OutboxEventType.TICKET_ISSUED,
            objectMapper.writeValueAsString(new TicketIssuedPayload(300L, 200L, 20L, "session-1", 10L, "A", "1", "10", LocalDateTime.of(2026, 3, 17, 10, 1))),
            LocalDateTime.of(2026, 3, 17, 10, 1)
        );
        ticketIssued.setId(1L);

        when(projectionCheckpointRepository.findById(consumerName)).thenReturn(Optional.of(ProjectionCheckpoint.initialize(consumerName, LocalDateTime.now())));
        when(outboxEventRepository.findByIdGreaterThanOrderByIdAsc(any(Long.class), any(Pageable.class)))
            .thenReturn(List.of(ticketIssued));
        when(seatAvailabilityProjectionRepository.findById(10L)).thenReturn(Optional.of(seatProjection));
        when(seatAvailabilityProjectionRepository.save(any(SeatAvailabilityProjection.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userTicketProjectionRepository.findById(300L)).thenReturn(Optional.empty());
        when(userTicketProjectionRepository.save(any(UserTicketProjection.class))).thenThrow(new IllegalStateException("projection write failed"));

        assertThrows(IllegalStateException.class, () -> projectionConsumerService.processNextBatch(consumerName, 10));

        verify(projectionCheckpointRepository, never()).save(any(ProjectionCheckpoint.class));
    }

    @Test
    void processNextBatchFailsLoudlyForHoldExpiredUntilProjectionIsImplemented() throws Exception {
        String consumerName = ProjectionConsumerService.DEFAULT_CONSUMER_NAME;
        OutboxEvent holdExpired = OutboxEvent.create(
            OutboxAggregateType.HOLD,
            100L,
            OutboxEventType.HOLD_EXPIRED,
            "{}",
            LocalDateTime.of(2026, 3, 17, 10, 2)
        );
        holdExpired.setId(1L);

        when(projectionCheckpointRepository.findById(consumerName)).thenReturn(Optional.of(ProjectionCheckpoint.initialize(consumerName, LocalDateTime.now())));
        when(outboxEventRepository.findByIdGreaterThanOrderByIdAsc(any(Long.class), any(Pageable.class)))
            .thenReturn(List.of(holdExpired));

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
            () -> projectionConsumerService.processNextBatch(consumerName, 10));

        assertEquals("HOLD_EXPIRED projection not yet implemented", thrown.getMessage());
        verify(projectionCheckpointRepository, never()).save(any(ProjectionCheckpoint.class));
    }
}
