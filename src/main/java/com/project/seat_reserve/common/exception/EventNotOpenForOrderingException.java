package com.project.seat_reserve.common.exception;

public class EventNotOpenForOrderingException extends RuntimeException {
    public EventNotOpenForOrderingException(Long eventId) {
        super("Event is not open for ordering: " + eventId);
    }
}
