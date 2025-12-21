package com.project.seat_reserve.seat;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.project.seat_reserve.common.exception.EventNotFoundException;
import com.project.seat_reserve.event.EventRepository;
import com.project.seat_reserve.seat.dto.CreateSeatRequest;
import com.project.seat_reserve.seat.dto.SeatResponse;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

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
                .orElseThrow(() -> new EventNotFoundException(request.getEventId()))
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
