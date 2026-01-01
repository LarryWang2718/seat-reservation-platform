package com.project.seat_reserve.graphql.dto;

public record OrderView(
    Long id,
    Long eventId,
    String sessionId,
    String status,
    String createdAt
) {
}
