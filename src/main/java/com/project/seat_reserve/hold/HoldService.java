package com.project.seat_reserve.hold;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.project.seat_reserve.common.exception.HoldLimitExceededException;
import com.project.seat_reserve.common.exception.OrderNotFoundException;
import com.project.seat_reserve.common.exception.OrderNotPendingException;
import com.project.seat_reserve.common.exception.SeatAlreadyHeldByOrderException;
import com.project.seat_reserve.common.exception.SeatAlreadyHeldException;
import com.project.seat_reserve.common.exception.SeatAlreadySoldException;
import com.project.seat_reserve.common.exception.SeatNotFoundException;
import com.project.seat_reserve.common.exception.SeatOrderMismatchException;
import com.project.seat_reserve.hold.dto.CreateHoldRequest;
import com.project.seat_reserve.hold.dto.HoldResponse;
import com.project.seat_reserve.order.Order;
import com.project.seat_reserve.order.OrderRepository;
import com.project.seat_reserve.order.OrderStatus;
import com.project.seat_reserve.seat.Seat;
import com.project.seat_reserve.seat.SeatRepository;
import com.project.seat_reserve.ticket.TicketRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HoldService {
    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;
    private final HoldRepository holdRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public HoldResponse createHold(CreateHoldRequest createHoldRequest) {
        Order order = getRequiredOrder(createHoldRequest.getOrderId());
        Seat seat = getRequiredSeat(createHoldRequest.getSeatId());
        validateHoldRequest(order, seat);

        LocalDateTime currentTime = LocalDateTime.now();
        Hold hold = Hold.createHeld(order, seat, currentTime, currentTime.plusMinutes(5));
        return toResponse(holdRepository.save(hold));
    }

    public List<HoldResponse> getHoldsByOrderId(Long orderId) {
        return holdRepository.findByOrderId(orderId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    private Order getRequiredOrder(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private Seat getRequiredSeat(Long seatId) {
        return seatRepository.findById(seatId)
            .orElseThrow(() -> new SeatNotFoundException(seatId));
    }

    private void validateHoldRequest(Order order, Seat seat) {
        Long orderId = order.getId();
        Long seatId = seat.getId();

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderNotPendingException(orderId);
        }
        if (!seat.getEvent().getId().equals(order.getEvent().getId())) {
            throw new SeatOrderMismatchException(seatId, orderId);
        }
        if (holdRepository.countByOrderIdAndStatus(orderId, HoldStatus.HELD) >= 4) {
            throw new HoldLimitExceededException(orderId);
        }
        if (ticketRepository.existsBySeatId(seatId)) {
            throw new SeatAlreadySoldException(seatId);
        }
        if (holdRepository.existsBySeatIdAndOrderIdAndStatus(seatId, orderId, HoldStatus.HELD)) {
            throw new SeatAlreadyHeldByOrderException(seatId, orderId);
        }
        if (holdRepository.existsBySeatIdAndStatus(seatId, HoldStatus.HELD)) {
            throw new SeatAlreadyHeldException(seatId);
        }
    }

    private HoldResponse toResponse(Hold hold) {
        return new HoldResponse(hold.getId(), hold.getSeat().getId(), hold.getOrder().getId(), hold.getExpiresAt(), hold.getCreatedAt(), hold.getStatus());
    }
}
