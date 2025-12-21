package com.project.seat_reserve.common.exception;

public class InvalidEventTimeRangeException extends RuntimeException {
    public InvalidEventTimeRangeException() {
        super("Event end time must be after start time");
    }
}
