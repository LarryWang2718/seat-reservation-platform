package com.project.seat_reserve.common.exception;

public class EventNotFoundException extends RuntimeException {
    public EventNotFoundException(Long eventId) {
        super("Event not found: " + eventId);
    }
}
