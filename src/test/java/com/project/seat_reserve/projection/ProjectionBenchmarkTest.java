package com.project.seat_reserve.projection;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.seat_reserve.event.Event;
import com.project.seat_reserve.event.EventRepository;
import com.project.seat_reserve.event.EventStatus;
import com.project.seat_reserve.hold.HoldRepository;
import com.project.seat_reserve.outbox.HoldCreatedPayload;
import com.project.seat_reserve.outbox.OutboxAggregateType;
import com.project.seat_reserve.outbox.OutboxEvent;
import com.project.seat_reserve.outbox.OutboxEventRepository;
import com.project.seat_reserve.outbox.OutboxEventType;
import com.project.seat_reserve.order.OrderRepository;
import com.project.seat_reserve.seat.Seat;
import com.project.seat_reserve.seat.SeatRepository;
import com.project.seat_reserve.ticket.TicketRepository;

@SpringBootTest
class ProjectionBenchmarkTest {
    private static final String BENCHMARK_CONSUMER = "benchmark-seat-projection";
    private static final int SEAT_COUNT = 500;

    @Autowired
    private ProjectionConsumerService projectionConsumerService;

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

    @Autowired
    private OutboxEventRepository outboxEventRepository;

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
    private ObjectMapper objectMapper;

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
    void benchmarkHoldCreatedProjectionThroughputForOneEvent() throws Exception {
        Event event = eventRepository.save(Event.create(
            "Projection Benchmark Event",
            LocalDateTime.now().plusDays(2),
            LocalDateTime.now().plusDays(2).plusHours(2),
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(4),
            "Benchmark Arena",
            EventStatus.ON_SALE
        ));

        List<Seat> seats = seatRepository.saveAll(IntStream.range(0, SEAT_COUNT)
            .mapToObj(index -> Seat.create(event, "A", "1", String.valueOf(index + 1)))
            .toList());

        LocalDateTime createdAt = LocalDateTime.now();
        seatAvailabilityProjectionRepository.saveAll(seats.stream()
            .map(seat -> SeatAvailabilityProjection.createAvailable(seat, createdAt))
            .toList());

        List<OutboxEvent> outboxEvents = IntStream.range(0, SEAT_COUNT)
            .mapToObj(index -> {
                Seat seat = seats.get(index);
                long holdId = 100_000L + index;
                long orderId = 200_000L + index;
                try {
                    return OutboxEvent.create(
                        OutboxAggregateType.HOLD,
                        holdId,
                        OutboxEventType.HOLD_CREATED,
                        objectMapper.writeValueAsString(new HoldCreatedPayload(
                            holdId,
                            orderId,
                            event.getId(),
                            seat.getId(),
                            "benchmark-session",
                            createdAt,
                            createdAt.plusMinutes(5)
                        )),
                        createdAt
                    );
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            })
            .toList();
        outboxEventRepository.saveAll(outboxEvents);

        long startNanos = System.nanoTime();
        int processed = projectionConsumerService.processNextBatch(BENCHMARK_CONSUMER, SEAT_COUNT);
        long elapsedNanos = System.nanoTime() - startNanos;

        double elapsedMs = elapsedNanos / 1_000_000.0;
        double updatesPerSecond = SEAT_COUNT / (elapsedNanos / 1_000_000_000.0);

        assertThat(processed).isEqualTo(SEAT_COUNT);
        assertThat(seatAvailabilityProjectionRepository.findAll())
            .allMatch(projection -> projection.getStatus() == SeatAvailabilityStatus.HELD);

        System.out.printf(
            "BENCHMARK hold_created_projection seats=%d elapsedMs=%.2f updatesPerSecond=%.2f%n",
            SEAT_COUNT,
            elapsedMs,
            updatesPerSecond
        );
    }
}
