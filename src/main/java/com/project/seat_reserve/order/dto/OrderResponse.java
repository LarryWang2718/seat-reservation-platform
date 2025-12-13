package com.project.seat_reserve.order.dto;

import java.time.LocalDateTime;

import com.project.seat_reserve.order.OrderStatus;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private Long eventId;
    private String sessionId;
    private OrderStatus status;
    private LocalDateTime createdAt;
}
