package com.project.seat_reserve.event.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateEventRequest {
    @NotBlank(message = "Name cannot be blank")
    private String name;

    @NotNull(message = "Start time cannot be null")
    private LocalDateTime startTime;

    @NotNull(message = "End time cannot be null")
    private LocalDateTime endTime;

    @NotNull(message = "Sale start time cannot be null")
    private LocalDateTime saleStartTime;

    @NotNull(message = "Sale end time cannot be null")
    private LocalDateTime saleEndTime;

    @NotBlank(message = "Location cannot be blank")
    private String location;
}
