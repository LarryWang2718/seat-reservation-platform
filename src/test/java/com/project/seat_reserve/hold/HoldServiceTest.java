package com.project.seat_reserve.hold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.seat_reserve.common.exception.HoldLimitExceededException;
import com.project.seat_reserve.common.exception.OrderNotFoundException;
import com.project.seat_reserve.common.exception.OrderNotPendingException;
import com.project.seat_reserve.common.exception.SeatAlreadyHeldByOrderException;
import com.project.seat_reserve.common.exception.SeatAlreadyHeldException;
import com.project.seat_reserve.common.exception.SeatAlreadySoldException;
import com.project.seat_reserve.common.exception.SeatNotFoundException;
import com.project.seat_reserve.common.exception.SeatOrderMismatchException;
import com.project.seat_reserve.event.Event;
import com.project.seat_reserve.hold.dto.CreateHoldRequest;
import com.project.seat_reserve.hold.dto.HoldResponse;
import com.project.seat_reserve.order.Order;
import com.project.seat_reserve.order.OrderRepository;
import com.project.seat_reserve.order.OrderStatus;
import com.project.seat_reserve.seat.Seat;
import com.project.seat_reserve.seat.SeatRepository;
import com.project.seat_reserve.ticket.TicketRepository;

@ExtendWith(MockitoExtension.class)
class HoldServiceTest {
    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private HoldRepository holdRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private HoldService holdService;

    @Captor
    private ArgumentCaptor<Hold> holdCaptor;

    @Test
    void createHoldBuildsHeldRecordFromRequest() {
        Event event = buildEvent(1L);
        Seat seat = buildSeat(10L, event);
        Order order = buildOrder(20L, event, OrderStatus.PENDING);

        CreateHoldRequest request = new CreateHoldRequest(10L, 20L);

        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime expiresAt = createdAt.plusMinutes(5);

        Hold savedHold = new Hold();
        savedHold.setId(100L);
        savedHold.setSeat(seat);
        savedHold.setOrder(order);
        savedHold.setCreatedAt(createdAt);
        savedHold.setExpiresAt(expiresAt);
        savedHold.setStatus(HoldStatus.HELD);

        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));
        when(seatRepository.findById(10L)).thenReturn(Optional.of(seat));
        when(holdRepository.save(holdCaptor.capture())).thenReturn(savedHold);

        HoldResponse response = holdService.createHold(request);
        Hold capturedHold = holdCaptor.getValue();

        assertEquals(seat, capturedHold.getSeat());
        assertEquals(order, capturedHold.getOrder());
        assertEquals(HoldStatus.HELD, capturedHold.getStatus());
        assertEquals(capturedHold.getCreatedAt().plusMinutes(5), capturedHold.getExpiresAt());

        assertEquals(100L, response.getId());
        assertEquals(10L, response.getSeatId());
        assertEquals(20L, response.getOrderId());
        assertEquals(HoldStatus.HELD, response.getStatus());
        assertEquals(createdAt, response.getCreatedAt());
        assertEquals(expiresAt, response.getExpiresAt());
    }

    @Test
    void createHoldRejectsMissingSeat() {
        Event event = buildEvent(1L);
        Order order = buildOrder(20L, event, OrderStatus.PENDING);

        CreateHoldRequest request = new CreateHoldRequest(99L, 20L);

        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));
        when(seatRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SeatNotFoundException.class, () -> holdService.createHold(request));
    }

    @Test
    void createHoldRejectsMissingOrder() {
        CreateHoldRequest request = new CreateHoldRequest(10L, 99L);

        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> holdService.createHold(request));
    }

    @Test
    void createHoldRejectsOrderThatIsNotPending() {
        Event event = buildEvent(1L);
        Seat seat = buildSeat(10L, event);
        Order order = buildOrder(20L, event, OrderStatus.COMPLETED);

        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));
        when(seatRepository.findById(10L)).thenReturn(Optional.of(seat));

        assertThrows(OrderNotPendingException.class, () -> holdService.createHold(new CreateHoldRequest(10L, 20L)));
    }

    @Test
    void createHoldRejectsSeatFromDifferentEventThanOrder() {
        Event orderEvent = buildEvent(1L);
        Event seatEvent = buildEvent(2L);
        Seat seat = buildSeat(10L, seatEvent);
        Order order = buildOrder(20L, orderEvent, OrderStatus.PENDING);

        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));
        when(seatRepository.findById(10L)).thenReturn(Optional.of(seat));

        assertThrows(SeatOrderMismatchException.class, () -> holdService.createHold(new CreateHoldRequest(10L, 20L)));
    }

    @Test
    void createHoldRejectsOrderAtHoldLimit() {
        Event event = buildEvent(1L);
        Seat seat = buildSeat(10L, event);
        Order order = buildOrder(20L, event, OrderStatus.PENDING);

        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));
        when(seatRepository.findById(10L)).thenReturn(Optional.of(seat));
        when(holdRepository.countByOrderIdAndStatus(20L, HoldStatus.HELD)).thenReturn(4L);

        assertThrows(HoldLimitExceededException.class, () -> holdService.createHold(new CreateHoldRequest(10L, 20L)));
    }

    @Test
    void createHoldRejectsSoldSeat() {
        Event event = buildEvent(1L);
        Seat seat = buildSeat(10L, event);
        Order order = buildOrder(20L, event, OrderStatus.PENDING);

        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));
        when(seatRepository.findById(10L)).thenReturn(Optional.of(seat));
        when(ticketRepository.existsBySeatId(10L)).thenReturn(true);

        assertThrows(SeatAlreadySoldException.class, () -> holdService.createHold(new CreateHoldRequest(10L, 20L)));
    }

    @Test
    void createHoldRejectsSeatAlreadyHeldBySameOrder() {
        Event event = buildEvent(1L);
        Seat seat = buildSeat(10L, event);
        Order order = buildOrder(20L, event, OrderStatus.PENDING);

        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));
        when(seatRepository.findById(10L)).thenReturn(Optional.of(seat));
        when(holdRepository.existsBySeatIdAndOrderIdAndStatus(10L, 20L, HoldStatus.HELD)).thenReturn(true);

        assertThrows(SeatAlreadyHeldByOrderException.class, () -> holdService.createHold(new CreateHoldRequest(10L, 20L)));
    }

    @Test
    void createHoldRejectsSeatAlreadyHeldByAnotherOrder() {
        Event event = buildEvent(1L);
        Seat seat = buildSeat(10L, event);
        Order order = buildOrder(20L, event, OrderStatus.PENDING);

        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));
        when(seatRepository.findById(10L)).thenReturn(Optional.of(seat));
        when(holdRepository.existsBySeatIdAndStatus(10L, HoldStatus.HELD)).thenReturn(true);

        assertThrows(SeatAlreadyHeldException.class, () -> holdService.createHold(new CreateHoldRequest(10L, 20L)));
    }

    @Test
    void getHoldsByOrderIdMapsRepositoryResults() {
        Event event = buildEvent(1L);
        Seat seat = buildSeat(10L, event);
        Order order = buildOrder(20L, event, OrderStatus.PENDING);

        Hold firstHold = new Hold(
            1L,
            seat,
            order,
            LocalDateTime.of(2025, 12, 10, 17, 25, 50),
            LocalDateTime.of(2025, 12, 10, 17, 20, 50),
            HoldStatus.HELD
        );

        Hold secondHold = new Hold(
            2L,
            seat,
            order,
            LocalDateTime.of(2025, 12, 10, 17, 30, 50),
            LocalDateTime.of(2025, 12, 10, 17, 25, 50),
            HoldStatus.EXPIRED
        );

        when(holdRepository.findByOrderId(20L)).thenReturn(List.of(firstHold, secondHold));

        List<HoldResponse> response = holdService.getHoldsByOrderId(20L);

        assertEquals(2, response.size());
        assertEquals(1L, response.get(0).getId());
        assertEquals(HoldStatus.HELD, response.get(0).getStatus());
        assertEquals(2L, response.get(1).getId());
        assertEquals(HoldStatus.EXPIRED, response.get(1).getStatus());
    }

    private Event buildEvent(Long eventId) {
        Event event = new Event();
        event.setId(eventId);
        return event;
    }

    private Seat buildSeat(Long seatId, Event event) {
        Seat seat = new Seat();
        seat.setId(seatId);
        seat.setEvent(event);
        seat.setSection("A");
        seat.setRowLabel("1");
        seat.setSeatNumber("5");
        return seat;
    }

    private Order buildOrder(Long orderId, Event event, OrderStatus status) {
        Order order = new Order();
        order.setId(orderId);
        order.setEvent(event);
        order.setSessionId("session-123");
        order.setStatus(status);
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }
}
