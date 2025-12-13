package com.project.seat_reserve.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    @NotBlank(message = "Session ID cannot be null")
    private String sessionId;

    @NotNull(message = "Event ID cannot be null")
    private Long eventId;
}
