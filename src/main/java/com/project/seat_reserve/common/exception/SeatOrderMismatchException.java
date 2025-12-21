package com.project.seat_reserve.common.exception;

public class SeatOrderMismatchException extends RuntimeException {
    public SeatOrderMismatchException(Long seatId, Long orderId) {
        super("Seat " + seatId + " does not belong to the same event as order " + orderId);
    }
}
