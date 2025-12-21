package com.project.seat_reserve.hold.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateHoldRequest {
    @NotNull(message = "Seat ID cannot be null")
    private Long seatId;

    @NotNull(message = "Order ID cannot be null")
    private Long orderId;
}
