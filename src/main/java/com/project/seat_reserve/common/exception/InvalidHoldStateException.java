package com.project.seat_reserve.common.exception;

import com.project.seat_reserve.hold.HoldStatus;

public class InvalidHoldStateException extends RuntimeException {
    public InvalidHoldStateException(Long holdId, HoldStatus status) {
        super("Hold " + holdId + " is in an invalid state: " + status);
    }
}
