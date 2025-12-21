package com.project.seat_reserve.common.exception;

public class SeatAlreadyHeldByOrderException extends RuntimeException {
    public SeatAlreadyHeldByOrderException(Long seatId, Long orderId) {
        super("Seat " + seatId + " is already held by order " + orderId);
    }
}
