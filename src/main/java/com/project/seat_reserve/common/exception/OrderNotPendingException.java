package com.project.seat_reserve.common.exception;

public class OrderNotPendingException extends RuntimeException {
    public OrderNotPendingException(Long orderId) {
        super("Order is not pending: " + orderId);
    }
}
