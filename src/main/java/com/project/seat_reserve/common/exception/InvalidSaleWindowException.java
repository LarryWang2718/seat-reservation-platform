package com.project.seat_reserve.common.exception;

public class InvalidSaleWindowException extends RuntimeException {
    public InvalidSaleWindowException() {
        super("Sale end time must be after sale start time");
    }
}
