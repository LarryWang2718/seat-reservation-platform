package com.project.seat_reserve.common.exception;

public class SeatAlreadyHeldException extends RuntimeException {
    public SeatAlreadyHeldException(Long seatId) {
        super("Seat is already actively held: " + seatId);
    }
}
