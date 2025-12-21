package com.project.seat_reserve.ticket;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.project.seat_reserve.common.exception.OrderNotFoundException;
import com.project.seat_reserve.common.exception.SeatNotFoundException;
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
    private final SeatRepository seatRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setSeat(seatRepository.findById(request.getSeatId()).orElseThrow(() -> new SeatNotFoundException(request.getSeatId())));
        ticket.setOrder(orderRepository.findById(request.getOrderId()).orElseThrow(() -> new OrderNotFoundException(request.getOrderId())));
        ticket.setCreatedAt(LocalDateTime.now());
        return toResponse(ticketRepository.save(ticket));
    }

    private TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
            ticket.getId(),
            ticket.getSeat().getId(),
            ticket.getOrder().getId(),
            ticket.getCreatedAt()
        );
    }
}
