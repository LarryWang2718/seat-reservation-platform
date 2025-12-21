package com.project.seat_reserve.common.exception;

public class EventSaleWindowClosedException extends RuntimeException {
    public EventSaleWindowClosedException() {
        super("Cannot create an event whose sale window has already ended");
    }
}
