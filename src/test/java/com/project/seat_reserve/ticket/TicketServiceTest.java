package com.project.seat_reserve.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.seat_reserve.common.exception.OrderNotFoundException;
import com.project.seat_reserve.common.exception.SeatNotFoundException;
import com.project.seat_reserve.event.Event;
import com.project.seat_reserve.order.Order;
import com.project.seat_reserve.order.OrderRepository;
import com.project.seat_reserve.order.OrderStatus;
import com.project.seat_reserve.seat.Seat;
import com.project.seat_reserve.seat.SeatRepository;
import com.project.seat_reserve.ticket.dto.CreateTicketRequest;
import com.project.seat_reserve.ticket.dto.TicketResponse;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {
    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private TicketService ticketService;

    @Captor
    private ArgumentCaptor<Ticket> ticketCaptor;

    @Test
    void createTicketBuildsTicketFromSeatAndOrder() {
        Event event = new Event();
        event.setId(1L);

        Seat seat = new Seat();
        seat.setId(10L);
        seat.setEvent(event);
        seat.setSection("A");
        seat.setRowLabel("1");
        seat.setSeatNumber("5");

        Order order = new Order();
        order.setId(20L);
        order.setEvent(event);
        order.setSessionId("session-123");
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        CreateTicketRequest request = new CreateTicketRequest(10L, 20L);

        LocalDateTime createdAt = LocalDateTime.now();

        Ticket savedTicket = new Ticket();
        savedTicket.setId(100L);
        savedTicket.setSeat(seat);
        savedTicket.setOrder(order);
        savedTicket.setCreatedAt(createdAt);

        when(seatRepository.findById(10L)).thenReturn(Optional.of(seat));
        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));
        when(ticketRepository.save(ticketCaptor.capture())).thenReturn(savedTicket);

        TicketResponse response = ticketService.createTicket(request);
        Ticket capturedTicket = ticketCaptor.getValue();

        assertEquals(seat, capturedTicket.getSeat());
        assertEquals(order, capturedTicket.getOrder());

        assertEquals(100L, response.getId());
        assertEquals(10L, response.getSeatId());
        assertEquals(20L, response.getOrderId());
        assertEquals(createdAt, response.getCreatedAt());
    }

    @Test
    void createTicketRejectsMissingSeat() {
        CreateTicketRequest request = new CreateTicketRequest(99L, 20L);

        when(seatRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SeatNotFoundException.class, () -> ticketService.createTicket(request));
    }

    @Test
    void createTicketRejectsMissingOrder() {
        Event event = new Event();
        event.setId(1L);

        Seat seat = new Seat();
        seat.setId(10L);
        seat.setEvent(event);

        CreateTicketRequest request = new CreateTicketRequest(10L, 99L);

        when(seatRepository.findById(10L)).thenReturn(Optional.of(seat));
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> ticketService.createTicket(request));
    }
}
