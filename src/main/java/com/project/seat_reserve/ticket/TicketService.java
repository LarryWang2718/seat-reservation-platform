package com.project.seat_reserve.ticket;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.project.seat_reserve.common.exception.OrderNotFoundException;
import com.project.seat_reserve.common.exception.OrderNotPendingException;
import com.project.seat_reserve.common.exception.SeatAlreadySoldException;
import com.project.seat_reserve.common.exception.SeatNotFoundException;
import com.project.seat_reserve.common.exception.SeatOrderMismatchException;
import com.project.seat_reserve.order.Order;
import com.project.seat_reserve.order.OrderRepository;
import com.project.seat_reserve.order.OrderStatus;
import com.project.seat_reserve.seat.Seat;
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
        Seat seat = getRequiredSeat(request.getSeatId());
        Order order = getRequiredOrder(request.getOrderId());
        validateTicketRequest(seat, order);
        Ticket ticket = new Ticket();
        ticket.setSeat(seatRepository.findById(request.getSeatId()).orElseThrow(() -> new SeatNotFoundException(request.getSeatId())));
        ticket.setOrder(orderRepository.findById(request.getOrderId()).orElseThrow(() -> new OrderNotFoundException(request.getOrderId())));
        ticket.setCreatedAt(LocalDateTime.now());
        return toResponse(ticketRepository.save(ticket));
    }

    private Seat getRequiredSeat(Long seatId) {
        return seatRepository.findById(seatId)
            .orElseThrow(() -> new SeatNotFoundException(seatId));
    }

    private Order getRequiredOrder(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private void validateTicketRequest(Seat seat, Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderNotPendingException(order.getId());
        }
        if (!seat.getEvent().getId().equals(order.getEvent().getId())) {
            throw new SeatOrderMismatchException(seat.getId(), order.getId());
        }
        if (ticketRepository.existsBySeatId(seat.getId())) {
            throw new SeatAlreadySoldException(seat.getId());
        }
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
