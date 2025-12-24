package com.project.seat_reserve.order;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.project.seat_reserve.common.exception.ActiveOrderAlreadyExistsException;
import com.project.seat_reserve.common.exception.EventNotFoundException;
import com.project.seat_reserve.common.exception.EventNotOpenForOrderingException;
import com.project.seat_reserve.common.exception.EventSaleWindowClosedException;
import com.project.seat_reserve.common.exception.InvalidHoldState;
import com.project.seat_reserve.common.exception.InvalidSessionIdException;
import com.project.seat_reserve.common.exception.NoActiveHoldsForOrderException;
import com.project.seat_reserve.common.exception.OrderNotFoundException;
import com.project.seat_reserve.common.exception.OrderNotPendingException;
import com.project.seat_reserve.common.exception.SeatAlreadySoldException;
import com.project.seat_reserve.event.Event;
import com.project.seat_reserve.event.EventRepository;
import com.project.seat_reserve.event.EventStatus;
import com.project.seat_reserve.hold.Hold;
import com.project.seat_reserve.hold.HoldRepository;
import com.project.seat_reserve.hold.HoldStatus;
import com.project.seat_reserve.order.dto.CreateOrderRequest;
import com.project.seat_reserve.order.dto.OrderResponse;
import com.project.seat_reserve.ticket.Ticket;
import com.project.seat_reserve.ticket.TicketRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final EventRepository eventRepository;
    private final HoldRepository holdRepository;
    private final TicketRepository ticketRepository;
    private final OrderCancellationService orderCancellationService;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Event event = eventRepository.findById(request.getEventId())
            .orElseThrow(() -> new EventNotFoundException(request.getEventId()));
        validateOrderRequest(request, event);

        Order order = Order.createPending(event, request.getSessionId(), LocalDateTime.now());
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse confirmOrder(Long orderId) {
        Order order = loadPendingOrder(orderId);

        try {
            List<Hold> holds = loadConfirmableHolds(orderId);
            LocalDateTime confirmationTime = LocalDateTime.now();
            List<Ticket> tickets = holds.stream()
                .map(hold -> Ticket.createForOrder(hold.getSeat(), order, confirmationTime))
                .toList();

            ticketRepository.saveAll(tickets);
            holds.forEach(Hold::markConfirmed);
            order.markCompleted();

            return toResponse(order);
        } catch (RuntimeException exception) {
            orderCancellationService.cancelOrder(orderId);
            throw exception;
        }
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
        if (holds.isEmpty()) {
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
                throw new InvalidHoldState(hold.getId(), HoldStatus.EXPIRED);
            }
            if (holdStatus != HoldStatus.HELD) {
                throw new InvalidHoldState(hold.getId(), holdStatus);
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
            throw new EventSaleWindowClosedException();
        }
        if (now.isBefore(event.getSaleStartTime())
            || event.getStatus() == EventStatus.CANCELLED
            || event.getStatus() == EventStatus.SOLD_OUT
            || event.getStatus() == EventStatus.ENDED) {
            throw new EventNotOpenForOrderingException(event.getId());
        }
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
