package com.project.seat_reserve.graphql.dto;

public record UserTicketView(
    Long ticketId,
    Long orderId,
    Long eventId,
    String sessionId,
    Long seatId,
    String section,
    String rowLabel,
    String seatNumber,
    String issuedAt
) {
}
