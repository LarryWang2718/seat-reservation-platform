package com.project.seat_reserve.common.exception;

public class InvalidSessionIdException extends RuntimeException {
    public InvalidSessionIdException() {
        super("Session ID cannot be blank");
    }
}
