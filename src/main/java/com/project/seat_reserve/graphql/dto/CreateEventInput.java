package com.project.seat_reserve.graphql.dto;

public record CreateEventInput(
    String name,
    String startTime,
    String endTime,
    String saleStartTime,
    String saleEndTime,
    String location
) {
}
