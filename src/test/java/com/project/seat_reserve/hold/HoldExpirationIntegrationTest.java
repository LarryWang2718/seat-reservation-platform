package com.project.seat_reserve.hold;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.project.seat_reserve.event.Event;
import com.project.seat_reserve.event.EventRepository;
import com.project.seat_reserve.event.EventStatus;
import com.project.seat_reserve.hold.dto.CreateHoldRequest;
import com.project.seat_reserve.order.Order;
import com.project.seat_reserve.order.OrderRepository;
import com.project.seat_reserve.order.OrderStatus;
import com.project.seat_reserve.outbox.OutboxAggregateType;
import com.project.seat_reserve.outbox.OutboxEvent;
import com.project.seat_reserve.outbox.OutboxEventRepository;
import com.project.seat_reserve.outbox.OutboxEventType;
import com.project.seat_reserve.projection.ProjectionCheckpointRepository;
import com.project.seat_reserve.projection.ProjectionConsumerService;
import com.project.seat_reserve.projection.ProjectionDeadLetterRepository;
import com.project.seat_reserve.projection.ProjectionEventFailureRepository;
import com.project.seat_reserve.projection.SeatAvailabilityProjection;
import com.project.seat_reserve.projection.SeatAvailabilityProjectionRepository;
import com.project.seat_reserve.projection.SeatAvailabilityStatus;
import com.project.seat_reserve.projection.UserTicketProjectionRepository;
import com.project.seat_reserve.seat.SeatRepository;
import com.project.seat_reserve.seat.SeatService;
import com.project.seat_reserve.seat.dto.CreateSeatRequest;
import com.project.seat_reserve.ticket.TicketRepository;

@SpringBootTest(properties = {
    "app.projection-consumer.initial-delay-ms=600000",
    "app.projection-consumer.fixed-delay-ms=600000",
    "app.hold-expiration.initial-delay-ms=600000",
    "app.hold-expiration.fixed-delay-ms=600000"
})
class HoldExpirationIntegrationTest {
    @Autowired
    private HoldService holdService;

    @Autowired
    private HoldExpirationService holdExpirationService;

    @Autowired
    private ProjectionConsumerService projectionConsumerService;

    @Autowired
    private SeatService seatService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private HoldRepository holdRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ProjectionCheckpointRepository projectionCheckpointRepository;

    @Autowired
    private ProjectionEventFailureRepository projectionEventFailureRepository;

    @Autowired
    private ProjectionDeadLetterRepository projectionDeadLetterRepository;

    @Autowired
    private SeatAvailabilityProjectionRepository seatAvailabilityProjectionRepository;

    @Autowired
    private UserTicketProjectionRepository userTicketProjectionRepository;

    @BeforeEach
    void setUp() {
        userTicketProjectionRepository.deleteAllInBatch();
        seatAvailabilityProjectionRepository.deleteAllInBatch();
        projectionDeadLetterRepository.deleteAllInBatch();
        projectionEventFailureRepository.deleteAllInBatch();
        projectionCheckpointRepository.deleteAllInBatch();
        ticketRepository.deleteAllInBatch();
        outboxEventRepository.deleteAllInBatch();
        holdRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        seatRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
    }

    @Test
    void expireStaleHoldPublishesOutboxEventAndRestoresSeatAvailabilityProjection() {
        Event event = createEvent();
        Long seatId = seatService.createSeat(new CreateSeatRequest(event.getId(), "A", "1", "10")).getId();
        Order order = createOrder(event, "expiration-integration-session");

        holdService.createHold(new CreateHoldRequest(seatId, order.getId()));
        assertThat(projectionConsumerService.processNextBatch(ProjectionConsumerService.DEFAULT_CONSUMER_NAME, 10)).isEqualTo(1);

        Hold hold = holdRepository.findByOrderId(order.getId()).get(0);
        SeatAvailabilityProjection heldProjection = seatAvailabilityProjectionRepository.findById(seatId).orElseThrow();
        assertThat(heldProjection.getStatus()).isEqualTo(SeatAvailabilityStatus.HELD);
        assertThat(heldProjection.getHoldId()).isEqualTo(hold.getId());

        hold.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        holdRepository.save(hold);

        assertThat(holdExpirationService.expireStaleHolds(10)).isEqualTo(1);

        Hold expiredHold = holdRepository.findById(hold.getId()).orElseThrow();
        assertThat(expiredHold.getStatus()).isEqualTo(HoldStatus.EXPIRED);

        List<OutboxEvent> holdOutboxEvents = outboxEventRepository
            .findByAggregateTypeAndAggregateIdOrderByIdAsc(OutboxAggregateType.HOLD, hold.getId())
            .stream()
            .sorted(Comparator.comparing(OutboxEvent::getId))
            .toList();
        assertThat(holdOutboxEvents).hasSize(2);
        assertThat(holdOutboxEvents.get(0).getEventType()).isEqualTo(OutboxEventType.HOLD_CREATED);
        assertThat(holdOutboxEvents.get(1).getEventType()).isEqualTo(OutboxEventType.HOLD_EXPIRED);

        assertThat(projectionConsumerService.processNextBatch(ProjectionConsumerService.DEFAULT_CONSUMER_NAME, 10)).isEqualTo(1);

        SeatAvailabilityProjection availableProjection = seatAvailabilityProjectionRepository.findById(seatId).orElseThrow();
        assertThat(availableProjection.getStatus()).isEqualTo(SeatAvailabilityStatus.AVAILABLE);
        assertThat(availableProjection.getHoldId()).isNull();
        assertThat(availableProjection.getSessionId()).isNull();
        assertThat(projectionCheckpointRepository.findById(ProjectionConsumerService.DEFAULT_CONSUMER_NAME).orElseThrow().getLastProcessedEventId())
            .isEqualTo(holdOutboxEvents.get(1).getId());
        assertThat(projectionDeadLetterRepository.findAll()).isEmpty();
        assertThat(projectionEventFailureRepository.findAll()).isEmpty();
    }

    private Event createEvent() {
        Event event = Event.create(
            "Hold Expiration Integration Event",
            LocalDateTime.now().plusDays(2),
            LocalDateTime.now().plusDays(2).plusHours(2),
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(4),
            "Integration Arena",
            EventStatus.ON_SALE
        );
        return eventRepository.save(event);
    }

    private Order createOrder(Event event, String sessionId) {
        Order order = Order.createPending(event, sessionId, LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        return orderRepository.save(order);
    }
}
