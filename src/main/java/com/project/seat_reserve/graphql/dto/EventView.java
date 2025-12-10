package com.project.seat_reserve.graphql.dto;

public record EventView(
    Long id,
    String name,
    String startTime,
    String endTime,
    String saleStartTime,
    String saleEndTime,
    String location,
    String status
) {
}
