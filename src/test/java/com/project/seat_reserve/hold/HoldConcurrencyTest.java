package com.project.seat_reserve.hold;

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

import com.project.seat_reserve.common.exception.SeatAlreadyHeldException;
import com.project.seat_reserve.event.Event;
import com.project.seat_reserve.event.EventRepository;
import com.project.seat_reserve.event.EventStatus;
import com.project.seat_reserve.hold.dto.CreateHoldRequest;
import com.project.seat_reserve.hold.dto.HoldResponse;
import com.project.seat_reserve.order.Order;
import com.project.seat_reserve.order.OrderRepository;
import com.project.seat_reserve.projection.SeatAvailabilityProjectionRepository;
import com.project.seat_reserve.projection.UserTicketProjectionRepository;
import com.project.seat_reserve.seat.Seat;
import com.project.seat_reserve.seat.SeatRepository;
import com.project.seat_reserve.ticket.TicketRepository;

@SpringBootTest
class HoldConcurrencyTest {

    @Autowired
    private HoldService holdService;

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
    private UserTicketProjectionRepository userTicketProjectionRepository;

    @Autowired
    private SeatAvailabilityProjectionRepository seatAvailabilityProjectionRepository;

    @BeforeEach
    void setUp() {
        userTicketProjectionRepository.deleteAllInBatch();
        seatAvailabilityProjectionRepository.deleteAllInBatch();
        ticketRepository.deleteAllInBatch();
        holdRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        seatRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
    }

    @Test
    void concurrentHoldsOnSameSeatResultInExactlyOneHeld() throws Exception {
        Event event = createEvent();
        Seat seat = createSeat(event);

        int threadCount = 5;
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            orders.add(createOrder(event, "session-" + i));
        }

        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Future<HoldResult>> futures = new ArrayList<>();
        for (Order order : orders) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();
                try {
                    HoldResponse response = holdService.createHold(
                        new CreateHoldRequest(seat.getId(), order.getId()));
                    return new HoldResult(response, null);
                } catch (Exception e) {
                    return new HoldResult(null, e);
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown();

        List<HoldResult> results = new ArrayList<>();
        for (Future<HoldResult> future : futures) {
            results.add(future.get());
        }
        executor.shutdown();

        long successCount = results.stream().filter(r -> r.response != null).count();
        long failureCount = results.stream().filter(r -> r.exception != null).count();

        assertThat(successCount).isEqualTo(1);
        assertThat(failureCount).isEqualTo(threadCount - 1);

        for (HoldResult result : results) {
            if (result.exception != null) {
                assertThat(result.exception).isInstanceOf(SeatAlreadyHeldException.class);
            }
        }

        List<Hold> heldHolds = holdRepository.findAll().stream()
            .filter(h -> h.getStatus() == HoldStatus.HELD)
            .toList();
        assertThat(heldHolds).hasSize(1);
        assertThat(heldHolds.get(0).getSeat().getId()).isEqualTo(seat.getId());
    }

    private Event createEvent() {
        Event event = Event.create(
            "Concurrency Test Event",
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
        Order order = Order.createPending(event, sessionId, LocalDateTime.now());
        return orderRepository.save(order);
    }

    private record HoldResult(HoldResponse response, Exception exception) {}
}
