package com.project.seat_reserve.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.project.seat_reserve.event.Event;
import com.project.seat_reserve.hold.Hold;
import com.project.seat_reserve.hold.HoldRepository;
import com.project.seat_reserve.hold.HoldStatus;
import com.project.seat_reserve.outbox.OutboxEventService;
import com.project.seat_reserve.seat.Seat;
import com.project.seat_reserve.ticket.Ticket;
import com.project.seat_reserve.ticket.TicketRepository;

@ExtendWith(MockitoExtension.class)
class CancellationCleanupServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private HoldRepository holdRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private OutboxEventService outboxEventService;

    @InjectMocks
    private CancellationCleanupService cancellationCleanupService;

    @Captor
    private ArgumentCaptor<Hold> holdCaptor;

    @Captor
    private ArgumentCaptor<List<Hold>> holdsCaptor;

    @Captor
    private ArgumentCaptor<List<Order>> ordersCaptor;

    @Captor
    private ArgumentCaptor<LocalDateTime> cleanupTimeCaptor;

    @Test
    void cleanupCancelledOrdersDeletesOldCancelledOrdersAndPublishesReleaseEvents() {
        Order cancelledOrder = buildOrder(10L, OrderStatus.CANCELLED, LocalDateTime.now().minusDays(2));
        Hold cancelledHold = buildHold(100L, cancelledOrder, 200L, HoldStatus.CANCELLED);
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

        when(orderRepository.findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(any(OrderStatus.class), any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(List.of(cancelledOrder));
        when(holdRepository.findByOrderIdIn(List.of(cancelledOrder.getId()))).thenReturn(List.of(cancelledHold));
        when(ticketRepository.findByOrderIdIn(List.of(cancelledOrder.getId()))).thenReturn(List.of());

        int deletedCount = cancellationCleanupService.cleanupCancelledOrders(50, cutoff);

        assertEquals(1, deletedCount);
        verify(outboxEventService, times(1)).publishHoldExpired(holdCaptor.capture(), cleanupTimeCaptor.capture());
        assertEquals(cancelledHold, holdCaptor.getValue());
        verify(holdRepository).deleteAllInBatch(holdsCaptor.capture());
        verify(orderRepository).deleteAllInBatch(ordersCaptor.capture());
        assertEquals(List.of(cancelledHold), holdsCaptor.getValue());
        assertEquals(List.of(cancelledOrder), ordersCaptor.getValue());
    }

    @Test
    void cleanupCancelledOrdersSkipsOrdersWithNonCancelledHoldsOrTickets() {
        Order cancelledWithHeld = buildOrder(10L, OrderStatus.CANCELLED, LocalDateTime.now().minusDays(2));
        Order cancelledWithTicket = buildOrder(11L, OrderStatus.CANCELLED, LocalDateTime.now().minusDays(2));
        Hold heldHold = buildHold(100L, cancelledWithHeld, 200L, HoldStatus.HELD);
        Hold cancelledHold = buildHold(101L, cancelledWithTicket, 201L, HoldStatus.CANCELLED);
        Ticket ticket = buildTicket(300L, cancelledWithTicket, 201L);
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

        when(orderRepository.findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(any(OrderStatus.class), any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(List.of(cancelledWithHeld, cancelledWithTicket));
        when(holdRepository.findByOrderIdIn(List.of(cancelledWithHeld.getId(), cancelledWithTicket.getId())))
            .thenReturn(List.of(heldHold, cancelledHold));
        when(ticketRepository.findByOrderIdIn(List.of(cancelledWithHeld.getId(), cancelledWithTicket.getId())))
            .thenReturn(List.of(ticket));

        int deletedCount = cancellationCleanupService.cleanupCancelledOrders(50, cutoff);

        assertEquals(0, deletedCount);
        verify(outboxEventService, never()).publishHoldExpired(any(Hold.class), any(LocalDateTime.class));
        verify(holdRepository, never()).deleteAllInBatch(any());
        verify(orderRepository, never()).deleteAllInBatch(any());
    }

    @Test
    void cleanupCancelledOrdersDoesNothingWhenNoRowsMatch() {
        when(orderRepository.findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(any(OrderStatus.class), any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(List.of());

        int deletedCount = cancellationCleanupService.cleanupCancelledOrders(25, LocalDateTime.now().minusHours(24));

        assertEquals(0, deletedCount);
        verify(holdRepository, never()).findByOrderIdIn(any());
        verify(ticketRepository, never()).findByOrderIdIn(any());
    }

    private Order buildOrder(Long orderId, OrderStatus status, LocalDateTime createdAt) {
        Event event = new Event();
        event.setId(1L);

        Order order = new Order();
        order.setId(orderId);
        order.setEvent(event);
        order.setSessionId("session-" + orderId);
        order.setStatus(status);
        order.setCreatedAt(createdAt);
        return order;
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
        hold.setCreatedAt(order.getCreatedAt());
        hold.setExpiresAt(order.getCreatedAt().plusMinutes(5));
        hold.setStatus(status);
        return hold;
    }

    private Ticket buildTicket(Long ticketId, Order order, Long seatId) {
        Seat seat = new Seat();
        seat.setId(seatId);
        seat.setEvent(order.getEvent());
        seat.setSection("A");
        seat.setRowLabel("1");
        seat.setSeatNumber(String.valueOf(seatId));

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setOrder(order);
        ticket.setSeat(seat);
        ticket.setCreatedAt(order.getCreatedAt().plusMinutes(1));
        return ticket;
    }
}
