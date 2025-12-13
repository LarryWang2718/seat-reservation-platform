package com.project.seat_reserve.event.dto;

import java.time.LocalDateTime;

import com.project.seat_reserve.event.EventStatus;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EventResponse {
    private Long id;
    private String name;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;
    private String location;
    private EventStatus status;
}
