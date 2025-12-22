package com.project.seat_reserve.order;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.project.seat_reserve.common.exception.ActiveOrderAlreadyExistsException;
import com.project.seat_reserve.common.exception.EventNotFoundException;
import com.project.seat_reserve.common.exception.EventNotOpenForOrderingException;
import com.project.seat_reserve.common.exception.EventSaleWindowClosedException;
import com.project.seat_reserve.common.exception.InvalidSessionIdException;
import com.project.seat_reserve.event.Event;
import com.project.seat_reserve.event.EventRepository;
import com.project.seat_reserve.event.EventStatus;
import com.project.seat_reserve.order.dto.CreateOrderRequest;
import com.project.seat_reserve.order.dto.OrderResponse;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final EventRepository eventRepository;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Event event = eventRepository.findById(request.getEventId())
            .orElseThrow(() -> new EventNotFoundException(request.getEventId()));
        validateOrderRequest(request, event);

        Order order = new Order();
        order.setSessionId(request.getSessionId());
        order.setEvent(event);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        return toResponse(orderRepository.save(order));
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
