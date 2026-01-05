package com.project.seat_reserve.outbox;

public enum OutboxEventType {
    HOLD_CREATED,
    HOLD_EXPIRED,
    ORDER_COMPLETED,
    TICKET_ISSUED
}
