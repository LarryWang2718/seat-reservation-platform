package com.project.seat_reserve.common.exception;

public class NoActiveHoldsForOrderException extends RuntimeException {
    public NoActiveHoldsForOrderException(Long orderId) {
        super("No active holds found for order " + orderId);
    }
}