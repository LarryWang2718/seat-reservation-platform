package com.project.seat_reserve.ticket;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.project.seat_reserve.event.EventRepository;
import com.project.seat_reserve.order.OrderRepository;
import com.project.seat_reserve.seat.SeatRepository;
import com.project.seat_reserve.ticket.dto.CreateTicketRequest;
import com.project.seat_reserve.ticket.dto.TicketResponse;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setEvent(eventRepository.findById(request.getEventId()).orElseThrow(() -> new IllegalArgumentException("Event not found")));
        ticket.setSeat(seatRepository.findById(request.getSeatId()).orElseThrow(() -> new IllegalArgumentException("Seat not found")));
        ticket.setOrder(orderRepository.findById(request.getOrderId()).orElseThrow(() -> new IllegalArgumentException("Order not found")));
        ticket.setCreatedAt(LocalDateTime.now());
        return toResponse(ticketRepository.save(ticket));
    }

    private TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
            ticket.getId(),
            ticket.getEvent().getId(),
            ticket.getSeat().getId(),
            ticket.getOrder().getId(),
            ticket.getCreatedAt()
        );
    }
}
