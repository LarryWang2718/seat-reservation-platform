package com.project.seat_reserve.graphql;

import jakarta.validation.Valid;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import com.project.seat_reserve.graphql.dto.CreateOrderInput;
import com.project.seat_reserve.graphql.dto.OrderView;
import com.project.seat_reserve.order.OrderService;
import com.project.seat_reserve.order.dto.CreateOrderRequest;
import com.project.seat_reserve.order.dto.OrderResponse;

import lombok.RequiredArgsConstructor;

@Controller
@Validated
@RequiredArgsConstructor
public class OrderGraphqlController {
    private final OrderService orderService;

    @MutationMapping
    public OrderView createOrder(@Argument("input") @Valid CreateOrderInput input) {
        CreateOrderRequest request = new CreateOrderRequest(input.sessionId(), input.eventId());
        return toView(orderService.createOrder(request));
    }

    @MutationMapping
    public OrderView confirmOrder(@Argument Long orderId) {
        return toView(orderService.confirmOrder(orderId));
    }

    private OrderView toView(OrderResponse response) {
        return new OrderView(
            response.getId(),
            response.getEventId(),
            response.getSessionId(),
            response.getStatus().name(),
            response.getCreatedAt().toString()
        );
    }
}
