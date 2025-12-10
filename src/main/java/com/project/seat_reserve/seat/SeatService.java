package com.project.seat_reserve.seat;

import com.project.seat_reserve.event.EventRepository;
import java.util.List;
import java.util.stream.Collectors;

import com.project.seat_reserve.seat.dto.CreateSeatRequest;
import com.project.seat_reserve.seat.dto.SeatResponse;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class SeatService {
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;

    @Transactional
    public SeatResponse createSeat(CreateSeatRequest request) {
        Seat seat = new Seat();
        seat.setEvent(
            eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found"))
        );
        seat.setSection(request.getSection());
        seat.setRowLabel(request.getRowLabel());
        seat.setSeatNumber(request.getSeatNumber());
  
        return toResponse(seatRepository.save(seat));
    }
  
    public List<SeatResponse> getSeatsByEventId(Long eventId) {
        return seatRepository.findByEventId(eventId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    

    private SeatResponse toResponse(Seat seat) {
        return new SeatResponse(seat.getId(), seat.getEvent().getId(), seat.getSection(), seat.getRowLabel(), seat.getSeatNumber());
    }
}
