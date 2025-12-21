package com.project.seat_reserve.ticket.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {
    private Long id;
    private Long seatId;
    private Long orderId;
    private LocalDateTime createdAt;
}
