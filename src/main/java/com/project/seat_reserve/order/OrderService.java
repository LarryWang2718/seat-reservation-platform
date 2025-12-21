package com.project.seat_reserve.order;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.project.seat_reserve.common.exception.EventNotFoundException;
import com.project.seat_reserve.event.EventRepository;
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
        Order order = new Order();
        order.setSessionId(request.getSessionId());
        order.setEvent(eventRepository.findById(request.getEventId()).orElseThrow(() -> new EventNotFoundException(request.getEventId())));
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        return toResponse(orderRepository.save(order));
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
