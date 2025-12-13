package com.project.seat_reserve.ticket.dto;

import jakarta.validation.constraints.NotNull;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketRequest {
    @NotNull(message = "Event ID cannot be null")
    private Long eventId;

    @NotNull(message = "Seat ID cannot be null")
    private Long seatId;

    @NotNull(message = "Order ID cannot be null")
    private Long orderId;
}
