package com.project.seat_reserve.outbox;

import java.time.LocalDateTime;

public record TicketIssuedPayload(
    Long ticketId,
    Long orderId,
    Long eventId,
    Long seatId,
    String section,
    String rowLabel,
    String seatNumber,
    LocalDateTime issuedAt
) {}
