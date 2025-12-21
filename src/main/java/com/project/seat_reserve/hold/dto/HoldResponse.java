package com.project.seat_reserve.hold.dto;

import java.time.LocalDateTime;

import com.project.seat_reserve.hold.HoldStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HoldResponse {
    private Long id;
    private Long seatId;
    private Long orderId;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private HoldStatus status;
}
