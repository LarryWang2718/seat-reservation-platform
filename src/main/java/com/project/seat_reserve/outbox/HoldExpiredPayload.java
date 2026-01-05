package com.project.seat_reserve.outbox;

import java.time.LocalDateTime;

public record HoldExpiredPayload(
    Long holdId,
    Long orderId,
    Long eventId,
    Long seatId,
    String sessionId,
    LocalDateTime expiredAt
) {}
