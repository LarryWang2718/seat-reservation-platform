package com.project.seat_reserve.common.exception;

public class SeatAlreadySoldException extends RuntimeException {
    public SeatAlreadySoldException(Long seatId) {
        super("Seat is already sold: " + seatId);
    }
}
