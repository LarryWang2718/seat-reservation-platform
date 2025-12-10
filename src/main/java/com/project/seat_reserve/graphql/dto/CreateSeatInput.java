package com.project.seat_reserve.graphql.dto;

public record CreateSeatInput(
    Long eventId,
    String section,
    String rowLabel,
    String seatNumber
) {
}
