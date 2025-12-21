package com.project.seat_reserve.graphql.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateEventInput(
    @NotBlank(message = "Name cannot be blank") String name,
    @NotBlank(message = "Start time cannot be blank") String startTime,
    @NotBlank(message = "End time cannot be blank") String endTime,
    @NotBlank(message = "Sale start time cannot be blank") String saleStartTime,
    @NotBlank(message = "Sale end time cannot be blank") String saleEndTime,
    @NotBlank(message = "Location cannot be blank") String location
) {
}
