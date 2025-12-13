package com.project.seat_reserve.ticket.dto;

import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {
    private Long id;
    private Long eventId;
    private Long seatId;
    private Long orderId;
    private LocalDateTime createdAt;
}
