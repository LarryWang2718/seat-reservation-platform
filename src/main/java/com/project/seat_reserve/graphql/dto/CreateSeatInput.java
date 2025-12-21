package com.project.seat_reserve.graphql.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSeatInput(
    @NotNull(message = "Event ID cannot be null") Long eventId,
    @NotBlank(message = "Section cannot be blank") String section,
    @NotBlank(message = "Row label cannot be blank") String rowLabel,
    @NotBlank(message = "Seat number cannot be blank") String seatNumber
) {
}
