package com.project.seat_reserve.graphql.dto;

import jakarta.validation.constraints.NotNull;

public record CreateHoldInput(
    @NotNull(message = "Seat ID cannot be null") Long seatId,
    @NotNull(message = "Order ID cannot be null") Long orderId
) {
}
