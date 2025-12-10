package com.project.seat_reserve.seat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateSeatRequest {
    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotBlank(message = "Section is required")
    private String section;

    @NotBlank(message = "Row label is required")
    private String rowLabel;

    @NotBlank(message = "Seat number is required")
    private String seatNumber;
}
