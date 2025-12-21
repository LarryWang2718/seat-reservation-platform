package com.project.seat_reserve.common.exception;

public class SeatNotFoundException extends RuntimeException {
    public SeatNotFoundException(Long seatId) {
        super("Seat not found: " + seatId);
    }
}
