package com.project.seat_reserve.graphql.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrderInput(
    @NotBlank(message = "Session ID cannot be blank") String sessionId,
    @NotNull(message = "Event ID cannot be null") Long eventId
) {
}
