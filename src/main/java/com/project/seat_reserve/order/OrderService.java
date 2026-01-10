package com.project.seat_reserve.order;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.project.seat_reserve.common.exception.ActiveOrderAlreadyExistsException;
import com.project.seat_reserve.common.exception.EventNotFoundException;
import com.project.seat_reserve.common.exception.EventNotOpenForOrderingException;
import com.project.seat_reserve.common.exception.InvalidHoldStateException;
import com.project.seat_reserve.common.exception.InvalidSessionIdException;
import com.project.seat_reserve.common.exception.NoActiveHoldsForOrderException;
import com.project.seat_reserve.common.exception.OrderCleanupFailedException;
import com.project.seat_reserve.common.exception.OrderNotFoundException;
import com.project.seat_reserve.common.exception.OrderNotPendingException;
import com.project.seat_reserve.common.exception.OrderSaleWindowClosedException;
import com.project.seat_reserve.common.exception.SeatAlreadySoldException;
import com.project.seat_reserve.event.Event;
import com.project.seat_reserve.event.EventRepository;
import com.project.seat_reserve.event.EventStatus;
import com.project.seat_reserve.hold.Hold;
import com.project.seat_reserve.hold.HoldRepository;
import com.project.seat_reserve.hold.HoldStatus;
import com.project.seat_reserve.outbox.OutboxEventService;
import com.project.seat_reserve.order.dto.CreateOrderRequest;
import com.project.seat_reserve.order.dto.OrderResponse;
import com.project.seat_reserve.ticket.Ticket;
import com.project.seat_reserve.ticket.TicketRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final String PENDING_ORDER_UNIQUE_INDEX = "uq_order_pending_session_event";

    private final OrderRepository orderRepository;
    private final EventRepository eventRepository;
    private final HoldRepository holdRepository;
    private final TicketRepository ticketRepository;
    private final OrderCancellationService orderCancellationService;
    private final OutboxEventService outboxEventService;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Event event = eventRepository.findById(request.getEventId())
            .orElseThrow(() -> new EventNotFoundException(request.getEventId()));
        validateOrderRequest(request, event);

        Order order = Order.createPending(event, request.getSessionId(), LocalDateTime.now());
        try {
            return toResponse(orderRepository.save(order));
        } catch (DataIntegrityViolationException e) {
            if (isPendingOrderUniqueViolation(e)) {
                throw new ActiveOrderAlreadyExistsException(request.getSessionId(), event.getId());
            }
            throw e;
        }
    }

    @Transactional
    public OrderResponse confirmOrder(Long orderId) {
        Order order = loadPendingOrder(orderId);
        List<Hold> holds;
        LocalDateTime confirmationTime;
        List<Ticket> savedTickets;

        try {
            holds = loadConfirmableHolds(orderId);
            confirmationTime = LocalDateTime.now();
            List<Ticket> tickets = holds.stream()
                .map(hold -> Ticket.createForOrder(hold.getSeat(), order, confirmationTime))
                .toList();

            savedTickets = ticketRepository.saveAll(tickets);
            holds.forEach(Hold::markConfirmed);
            order.markCompleted();
        } catch (RuntimeException exception) {
            try {
                orderCancellationService.cancelOrder(orderId);
            } catch (RuntimeException cancellationException) {
                throw new OrderCleanupFailedException(orderId, exception, cancellationException);
            }
            throw exception;
        }

        outboxEventService.publishOrderCompleted(order, holds, savedTickets, confirmationTime);
        return toResponse(order);
    }

    private Order loadPendingOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderNotPendingException(orderId);
        }
        return order;
    }

    private List<Hold> loadConfirmableHolds(Long orderId) {
        List<Hold> holds = holdRepository.findByOrderId(orderId);
        if (holds.isEmpty() || holds.stream().noneMatch(h -> h.getStatus() == HoldStatus.HELD)) {
            throw new NoActiveHoldsForOrderException(orderId);
        }
        validateConfirmableHolds(holds);
        return holds;
    }

    private void validateConfirmableHolds(List<Hold> holds) {
        LocalDateTime currentTime = LocalDateTime.now();
        for (Hold hold : holds) {
            HoldStatus holdStatus = hold.getStatus();
            if (hold.getExpiresAt().isBefore(currentTime)) {
                throw new InvalidHoldStateException(hold.getId(), HoldStatus.EXPIRED);
            }
            if (holdStatus != HoldStatus.HELD) {
                throw new InvalidHoldStateException(hold.getId(), holdStatus);
            }
            if (ticketRepository.existsBySeatId(hold.getSeat().getId())) {
                throw new SeatAlreadySoldException(hold.getSeat().getId());
            }
        }
    }

    private void validateOrderRequest(CreateOrderRequest request, Event event) {
        String sessionId = request.getSessionId();
        LocalDateTime now = LocalDateTime.now();

        if (sessionId == null || sessionId.isBlank()) {
            throw new InvalidSessionIdException();
        }
        if (orderRepository.existsBySessionIdAndEventIdAndStatus(sessionId, event.getId(), OrderStatus.PENDING)) {
            throw new ActiveOrderAlreadyExistsException(sessionId, event.getId());
        }
        if (!now.isBefore(event.getSaleEndTime())) {
            throw new OrderSaleWindowClosedException(event.getId());
        }
        if (now.isBefore(event.getSaleStartTime())
            || event.getStatus() == EventStatus.CANCELLED
            || event.getStatus() == EventStatus.SOLD_OUT
            || event.getStatus() == EventStatus.ENDED) {
            throw new EventNotOpenForOrderingException(event.getId());
        }
    }

    private boolean isPendingOrderUniqueViolation(DataIntegrityViolationException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolationException
                && PENDING_ORDER_UNIQUE_INDEX.equals(constraintViolationException.getConstraintName())) {
                return true;
            }
            current = current.getCause();
        }

        Throwable mostSpecificCause = exception.getMostSpecificCause();
        return mostSpecificCause != null
            && mostSpecificCause.getMessage() != null
            && mostSpecificCause.getMessage().contains(PENDING_ORDER_UNIQUE_INDEX);
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getEvent().getId(),
            order.getSessionId(),
            order.getStatus(),
            order.getCreatedAt()
        );
    }
}
