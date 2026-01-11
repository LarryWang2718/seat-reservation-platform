package com.project.seat_reserve.seat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {
    private Long id;
    private Long eventId;
    private String section;
    private String rowLabel;
    private String seatNumber;
    private String status;
}
