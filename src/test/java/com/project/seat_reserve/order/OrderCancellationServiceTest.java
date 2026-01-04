package com.project.seat_reserve.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.seat_reserve.common.exception.OrderNotFoundException;
import com.project.seat_reserve.event.Event;
import com.project.seat_reserve.event.EventStatus;
import com.project.seat_reserve.hold.Hold;
import com.project.seat_reserve.hold.HoldRepository;
import com.project.seat_reserve.hold.HoldStatus;
import com.project.seat_reserve.seat.Seat;

@ExtendWith(MockitoExtension.class)
class OrderCancellationServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private HoldRepository holdRepository;

    @InjectMocks
    private OrderCancellationService orderCancellationService;

    @Test
    void cancelOrderMarksOrderAndCancellableHoldsCancelled() {
        Event event = new Event();
        event.setId(1L);
        event.setStatus(EventStatus.ON_SALE);

        Order order = new Order();
        order.setId(10L);
        order.setEvent(event);
        order.setSessionId("session-123");
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        Hold firstHold = buildHold(100L, order, 200L, HoldStatus.HELD);
        Hold secondHold = buildHold(101L, order, 201L, HoldStatus.EXPIRED);

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(holdRepository.findByOrderId(10L)).thenReturn(List.of(firstHold, secondHold));

        orderCancellationService.cancelOrder(10L);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(HoldStatus.CANCELLED, firstHold.getStatus());
        assertEquals(HoldStatus.CANCELLED, secondHold.getStatus());
    }

    @Test
    void cancelOrderPreservesConfirmedHolds() {
        Event event = new Event();
        event.setId(1L);
        event.setStatus(EventStatus.ON_SALE);

        Order order = new Order();
        order.setId(10L);
        order.setEvent(event);
        order.setSessionId("session-123");
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        Hold heldHold = buildHold(100L, order, 200L, HoldStatus.HELD);
        Hold confirmedHold = buildHold(101L, order, 201L, HoldStatus.CONFIRMED);

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(holdRepository.findByOrderId(10L)).thenReturn(List.of(heldHold, confirmedHold));

        orderCancellationService.cancelOrder(10L);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(HoldStatus.CANCELLED, heldHold.getStatus());
        assertEquals(HoldStatus.CONFIRMED, confirmedHold.getStatus());
    }

    @Test
    void cancelOrderLeavesCompletedOrderUntouched() {
        Event event = new Event();
        event.setId(1L);
        event.setStatus(EventStatus.ON_SALE);

        Order order = new Order();
        order.setId(10L);
        order.setEvent(event);
        order.setSessionId("session-123");
        order.setStatus(OrderStatus.COMPLETED);
        order.setCreatedAt(LocalDateTime.now());

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        orderCancellationService.cancelOrder(10L);

        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }

    @Test
    void cancelOrderRejectsMissingOrder() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderCancellationService.cancelOrder(99L));
    }

    private Hold buildHold(Long holdId, Order order, Long seatId, HoldStatus status) {
        Seat seat = new Seat();
        seat.setId(seatId);
        seat.setEvent(order.getEvent());
        seat.setSection("A");
        seat.setRowLabel("1");
        seat.setSeatNumber(String.valueOf(seatId));

        Hold hold = new Hold();
        hold.setId(holdId);
        hold.setOrder(order);
        hold.setSeat(seat);
        hold.setCreatedAt(LocalDateTime.now());
        hold.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        hold.setStatus(status);
        return hold;
    }
}
