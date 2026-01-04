package com.project.seat_reserve.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.project.seat_reserve.common.exception.ActiveOrderAlreadyExistsException;
import com.project.seat_reserve.event.Event;
import com.project.seat_reserve.event.EventRepository;
import com.project.seat_reserve.event.EventStatus;
import com.project.seat_reserve.hold.Hold;
import com.project.seat_reserve.hold.HoldRepository;
import com.project.seat_reserve.hold.HoldStatus;
import com.project.seat_reserve.order.dto.CreateOrderRequest;
import com.project.seat_reserve.order.dto.OrderResponse;
import com.project.seat_reserve.seat.Seat;
import com.project.seat_reserve.seat.SeatRepository;
import com.project.seat_reserve.ticket.TicketRepository;

@SpringBootTest
class OrderConcurrencyTest {

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

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAllInBatch();
        holdRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        seatRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
    }

    @Test
    void concurrentOrderCreationsWithSameSessionAndEventResultInExactlyOne() throws Exception {
        Event event = createEvent();
        String sessionId = "race-session";

        int threadCount = 5;
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Future<OrderResult>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();
                try {
                    OrderResponse response = orderService.createOrder(
                        new CreateOrderRequest(sessionId, event.getId()));
                    return new OrderResult(response, null);
                } catch (Exception e) {
                    return new OrderResult(null, e);
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown();

        List<OrderResult> results = new ArrayList<>();
        for (Future<OrderResult> future : futures) {
            results.add(future.get());
        }
        executor.shutdown();

        long successCount = results.stream().filter(r -> r.response != null).count();
        long failureCount = results.stream().filter(r -> r.exception != null).count();

        assertThat(successCount).isEqualTo(1);
        assertThat(failureCount).isEqualTo(threadCount - 1);

        for (OrderResult result : results) {
            if (result.exception != null) {
                assertThat(result.exception).isInstanceOf(ActiveOrderAlreadyExistsException.class);
            }
        }

        List<Order> pendingOrders = orderRepository.findAll().stream()
            .filter(o -> o.getStatus() == OrderStatus.PENDING
                && o.getSessionId().equals(sessionId)
                && o.getEvent().getId().equals(event.getId()))
            .toList();
        assertThat(pendingOrders).hasSize(1);
    }

    @Test
    void concurrentConfirmationsForSameHeldSeatCreateExactlyOneTicket() throws Exception {
        Event event = createEvent();
        Seat seat = createSeat(event);
        Order order = createOrder(event, "confirm-race-session");
        Hold hold = createHold(order, seat);

        int threadCount = 5;
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Future<ConfirmResult>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();
                try {
                    OrderResponse response = orderService.confirmOrder(order.getId());
                    return new ConfirmResult(response, null);
                } catch (Exception e) {
                    return new ConfirmResult(null, e);
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown();

        List<ConfirmResult> results = new ArrayList<>();
        for (Future<ConfirmResult> future : futures) {
            results.add(future.get());
        }
        executor.shutdown();

        long successCount = results.stream().filter(r -> r.response != null).count();
        assertThat(successCount).isEqualTo(1);
        assertThat(ticketRepository.count()).isEqualTo(1);

        Order savedOrder = orderRepository.findById(order.getId()).orElseThrow();
        Hold savedHold = holdRepository.findById(hold.getId()).orElseThrow();

        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(savedHold.getStatus()).isEqualTo(HoldStatus.CONFIRMED);
        assertThat(ticketRepository.existsBySeatId(seat.getId())).isTrue();
    }

    private Event createEvent() {
        Event event = Event.create(
            "Order Concurrency Test Event",
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
        Seat seat = Seat.create(event, "A", "1", "10");
        return seatRepository.save(seat);
    }

    private Order createOrder(Event event, String sessionId) {
        return orderRepository.save(Order.createPending(event, sessionId, LocalDateTime.now()));
    }

    private Hold createHold(Order order, Seat seat) {
        return holdRepository.save(Hold.createHeld(order, seat, LocalDateTime.now(), LocalDateTime.now().plusMinutes(5)));
    }

    private record OrderResult(OrderResponse response, Exception exception) {}

    private record ConfirmResult(OrderResponse response, Exception exception) {}
}
