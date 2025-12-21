package com.project.seat_reserve.common.exception;

public class HoldLimitExceededException extends RuntimeException {
    public HoldLimitExceededException(Long orderId) {
        super("Order can have at most 4 active holds: " + orderId);
    }
}
