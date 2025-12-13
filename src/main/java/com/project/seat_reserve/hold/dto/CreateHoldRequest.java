package com.project.seat_reserve.hold.dto;

import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "Session ID cannot be null")
    private String sessionId;
}
