package com.project.seat_reserve.common.exception;

public class OrderCleanupFailedException extends RuntimeException {
    public OrderCleanupFailedException(Long orderId, Throwable cause, Throwable cleanupFailure) {
        super("Order " + orderId + " failed confirmation and cleanup also failed; order remains pending", cause);
        addSuppressed(cleanupFailure);
    }
}
