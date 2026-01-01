package com.project.seat_reserve.graphql.dto;

public record HoldView(
    Long id,
    Long seatId,
    Long orderId,
    String expiresAt,
    String createdAt,
    String status
) {
}
