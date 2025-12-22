package com.project.seat_reserve.common.exception;

public class ActiveOrderAlreadyExistsException extends RuntimeException {
    public ActiveOrderAlreadyExistsException(String sessionId, Long eventId) {
        super("An active order already exists for session " + sessionId + " and event " + eventId);
    }
}
