package com.project.seat_reserve.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import com.project.seat_reserve.event.Event;
import com.project.seat_reserve.event.EventRepository;
import com.project.seat_reserve.event.EventStatus;
import com.project.seat_reserve.hold.Hold;
import com.project.seat_reserve.hold.HoldRepository;
import com.project.seat_reserve.hold.HoldService;
import com.project.seat_reserve.hold.HoldStatus;
import com.project.seat_reserve.hold.dto.CreateHoldRequest;
import com.project.seat_reserve.order.Order;
import com.project.seat_reserve.order.OrderRepository;
import com.project.seat_reserve.order.OrderService;
import com.project.seat_reserve.order.OrderStatus;
import com.project.seat_reserve.seat.Seat;
import com.project.seat_reserve.seat.SeatRepository;
import com.project.seat_reserve.ticket.TicketRepository;

@SpringBootTest
class OutboxIntegrationTest {

    @Autowired
    private HoldService holdService;

    @Autowired
    private OrderService orderService;

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

    @SpyBean
    private OutboxEventService outboxEventService;

    @BeforeEach
    void setUp() {
        reset(outboxEventService);
        ticketRepository.deleteAllInBatch();
        outboxEventRepository.deleteAllInBatch();
        holdRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        seatRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
    }

    @Test
    void createHoldPersistsBusinessWriteAndOutboxRecordTogether() {
        Event event = createEvent();
        Seat seat = createSeat(event);
        Order order = createOrder(event, "hold-outbox-session");

        holdService.createHold(new CreateHoldRequest(seat.getId(), order.getId()));

        List<Hold> holds = holdRepository.findAll();
        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();

        assertThat(holds).hasSize(1);
        assertThat(holds.get(0).getStatus()).isEqualTo(HoldStatus.HELD);
        assertThat(outboxEvents).hasSize(1);
        assertThat(outboxEvents.get(0).getEventType()).isEqualTo(OutboxEventType.HOLD_CREATED);
        assertThat(outboxEvents.get(0).getAggregateType()).isEqualTo(OutboxAggregateType.HOLD);
        assertThat(outboxEvents.get(0).getAggregateId()).isEqualTo(holds.get(0).getId());
    }

    @Test
    void createHoldRollsBackBusinessWriteWhenOutboxWriteFails() {
        Event event = createEvent();
        Seat seat = createSeat(event);
        Order order = createOrder(event, "hold-outbox-failure-session");

        doThrow(new IllegalStateException("outbox failure"))
            .when(outboxEventService).publishHoldCreated(any(Hold.class));

        assertThrows(IllegalStateException.class,
            () -> holdService.createHold(new CreateHoldRequest(seat.getId(), order.getId())));

        assertThat(holdRepository.findAll()).isEmpty();
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    void confirmOrderPersistsBusinessWriteAndOutboxRecordsTogether() {
        Event event = createEvent();
        Seat firstSeat = createSeat(event, "A", "1", "10");
        Seat secondSeat = createSeat(event, "A", "1", "11");
        Order order = createOrder(event, "checkout-outbox-session");
        createHold(order, firstSeat);
        createHold(order, secondSeat);

        orderService.confirmOrder(order.getId());

        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll().stream()
            .sorted(Comparator.comparing(OutboxEvent::getId))
            .toList();

        assertThat(ticketRepository.findAll()).hasSize(2);
        assertThat(holdRepository.findByOrderId(order.getId()))
            .allMatch(hold -> hold.getStatus() == HoldStatus.CONFIRMED);
        assertThat(orderRepository.findById(order.getId())).get()
            .extracting(Order::getStatus)
            .isEqualTo(OrderStatus.COMPLETED);
        assertThat(outboxEvents).hasSize(3);
        assertThat(outboxEvents.stream().filter(eventRow -> eventRow.getEventType() == OutboxEventType.ORDER_COMPLETED)).hasSize(1);
        assertThat(outboxEvents.stream().filter(eventRow -> eventRow.getEventType() == OutboxEventType.TICKET_ISSUED)).hasSize(2);
    }

    @Test
    void confirmOrderDoesNotCommitCompletedWriteSetWhenOutboxWriteFails() {
        Event event = createEvent();
        Seat seat = createSeat(event);
        Order order = createOrder(event, "checkout-outbox-failure-session");
        createHold(order, seat);

        doThrow(new IllegalStateException("outbox failure"))
            .when(outboxEventService)
            .publishOrderCompleted(any(Order.class), anyList(), anyList(), any(LocalDateTime.class));

        assertThrows(IllegalStateException.class, () -> orderService.confirmOrder(order.getId()));

        assertThat(ticketRepository.findAll()).isEmpty();
        assertThat(outboxEventRepository.findAll()).isEmpty();
        assertThat(orderRepository.findById(order.getId())).get()
            .extracting(Order::getStatus)
            .isEqualTo(OrderStatus.CANCELLED);
        assertThat(holdRepository.findByOrderId(order.getId()))
            .allMatch(hold -> hold.getStatus() == HoldStatus.CANCELLED);
    }

    private Event createEvent() {
        Event event = Event.create(
            "Outbox Test Event",
            LocalDateTime.now().plusDays(2),
            LocalDateTime.now().plusDays(2).plusHours(2),
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(4),
            "Test Arena",
            EventStatus.ON_SALE
        );
        return eventRepository.save(event);
    }

    private Seat createSeat(Event event) {
        return createSeat(event, "A", "1", "10");
    }

    private Seat createSeat(Event event, String section, String rowLabel, String seatNumber) {
        return seatRepository.save(Seat.create(event, section, rowLabel, seatNumber));
    }

    private Order createOrder(Event event, String sessionId) {
        return orderRepository.save(Order.createPending(event, sessionId, LocalDateTime.now()));
    }

    private Hold createHold(Order order, Seat seat) {
        return holdRepository.save(Hold.createHeld(order, seat, LocalDateTime.now(), LocalDateTime.now().plusMinutes(5)));
    }
}

