package com.project.seat_reserve.hold;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
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
import com.project.seat_reserve.observability.ReservationMetrics;
import com.project.seat_reserve.outbox.OutboxEventService;
import com.project.seat_reserve.order.Order;
import com.project.seat_reserve.order.OrderRepository;
import com.project.seat_reserve.order.OrderStatus;
import com.project.seat_reserve.seat.Seat;
import com.project.seat_reserve.seat.SeatRepository;
import com.project.seat_reserve.ticket.TicketRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class HoldService {
    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;
    private final HoldRepository holdRepository;
    private final OrderRepository orderRepository;
    private final OutboxEventService outboxEventService;
    private final ReservationMetrics reservationMetrics;

    @Transactional
    public HoldResponse createHold(CreateHoldRequest createHoldRequest) {
        Long orderId = createHoldRequest.getOrderId();
        Long seatId = createHoldRequest.getSeatId();
        reservationMetrics.recordHoldAttempt();

        try {
            Order order = getRequiredOrder(orderId);
            Seat seat = getRequiredSeat(seatId);
            validateHoldRequest(order, seat);

            LocalDateTime currentTime = LocalDateTime.now();
            Hold hold = Hold.createHeld(order, seat, currentTime, currentTime.plusMinutes(5));
            Hold savedHold;
            try {
                savedHold = holdRepository.save(hold);
            } catch (DataIntegrityViolationException e) {
                throw new SeatAlreadyHeldException(seat.getId());
            }

            outboxEventService.publishHoldCreated(savedHold);
            reservationMetrics.recordHoldCreated();
            log.info("Created hold: holdId={}, orderId={}, seatId={}, sessionId={}, expiresAt={}",
                savedHold.getId(), orderId, seatId, order.getSessionId(), savedHold.getExpiresAt());
            return toResponse(savedHold);
        } catch (RuntimeException exception) {
            reservationMetrics.recordHoldFailure(exception.getClass().getSimpleName());
            if (isExpectedHoldFailure(exception)) {
                log.warn("Hold creation rejected: orderId={}, seatId={}, reason={}",
                    orderId, seatId, exception.getClass().getSimpleName());
            } else {
                log.error("Hold creation failed: orderId={}, seatId={}", orderId, seatId, exception);
            }
            throw exception;
        }
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

    private boolean isExpectedHoldFailure(RuntimeException exception) {
        return exception instanceof HoldLimitExceededException
            || exception instanceof OrderNotFoundException
            || exception instanceof OrderNotPendingException
            || exception instanceof SeatAlreadyHeldByOrderException
            || exception instanceof SeatAlreadyHeldException
            || exception instanceof SeatAlreadySoldException
            || exception instanceof SeatNotFoundException
            || exception instanceof SeatOrderMismatchException;
    }

    private HoldResponse toResponse(Hold hold) {
        return new HoldResponse(hold.getId(), hold.getSeat().getId(), hold.getOrder().getId(), hold.getExpiresAt(), hold.getCreatedAt(), hold.getStatus());
    }
}
