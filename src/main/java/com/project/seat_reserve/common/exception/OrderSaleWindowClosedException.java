package com.project.seat_reserve.common.exception;

public class OrderSaleWindowClosedException extends RuntimeException {
    public OrderSaleWindowClosedException(Long eventId) {
        super("Sale window has closed for event: " + eventId);
    }
}
