package com.project.seat_reserve.outbox;

import java.time.LocalDateTime;
import java.util.List;

public record OrderCompletedPayload(
    Long orderId,
    Long eventId,
    String sessionId,
    List<Long> holdIds,
    List<Long> ticketIds,
    LocalDateTime completedAt
) {}
