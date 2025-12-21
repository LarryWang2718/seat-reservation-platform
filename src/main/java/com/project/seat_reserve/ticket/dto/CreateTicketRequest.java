package com.project.seat_reserve.ticket.dto;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketRequest {
    @NotNull(message = "Seat ID cannot be null")
    private Long seatId;

    @NotNull(message = "Order ID cannot be null")
    private Long orderId;
}
