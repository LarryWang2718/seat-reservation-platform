package com.project.seat_reserve.hold;

import com.project.seat_reserve.seat.SeatRepository;

import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.project.seat_reserve.hold.dto.CreateHoldRequest;
import com.project.seat_reserve.hold.dto.HoldResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HoldService {
    private final SeatRepository seatRepository;
    private final HoldRepository holdRepository;

    @Transactional
    public HoldResponse createHold(CreateHoldRequest createHoldRequest) {
        Hold hold = new Hold();
        hold.setSeat(seatRepository.findById(createHoldRequest.getSeatId()).orElseThrow(() -> new IllegalArgumentException("Seat not found")));
        hold.setSessionId(createHoldRequest.getSessionId());
        LocalDateTime currentTime = LocalDateTime.now();
        hold.setExpiresAt(currentTime.plusMinutes(5));
        hold.setCreatedAt(currentTime);
        hold.setStatus(HoldStatus.HELD);
        return toResponse(holdRepository.save(hold));
    }

    private HoldResponse toResponse(Hold hold) {
        return new HoldResponse(hold.getId(), hold.getSeat().getId(), hold.getSessionId(), hold.getExpiresAt(), hold.getCreatedAt(), hold.getStatus());
    }

    public List<HoldResponse> getHoldsBySessionId(String sessionId) {
        return holdRepository.findBySessionId(sessionId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    
}
