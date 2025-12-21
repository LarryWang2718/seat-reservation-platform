package com.project.seat_reserve.common.exception;

public class SaleWindowAfterEventStartException extends RuntimeException {
    public SaleWindowAfterEventStartException() {
        super("Sale end time must be before event start time");
    }
}
