package com.project.seat_reserve.common.exception;

import com.project.seat_reserve.hold.HoldStatus;

public class InvalidHoldState extends RuntimeException {
    public InvalidHoldState(Long holdId, HoldStatus status) {
        super("Hold " + holdId + " is in an invalid state: " + status);
    }
}
