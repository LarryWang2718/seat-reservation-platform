package com.project.seat_reserve.seat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.project.seat_reserve.common.exception.EventNotFoundException;
import com.project.seat_reserve.event.Event;
import com.project.seat_reserve.event.EventRepository;
import com.project.seat_reserve.projection.SeatAvailabilityProjection;
import com.project.seat_reserve.projection.SeatAvailabilityProjectionRepository;
import com.project.seat_reserve.seat.dto.CreateSeatRequest;
import com.project.seat_reserve.seat.dto.SeatResponse;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class SeatService {
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final SeatAvailabilityProjectionRepository seatAvailabilityProjectionRepository;

    @Transactional
    public SeatResponse createSeat(CreateSeatRequest request) {
        Event event = eventRepository.findById(request.getEventId())
            .orElseThrow(() -> new EventNotFoundException(request.getEventId()));

        Seat seat = Seat.create(event, request.getSection(), request.getRowLabel(), request.getSeatNumber());
        Seat savedSeat = seatRepository.save(seat);
        seatAvailabilityProjectionRepository.save(SeatAvailabilityProjection.createAvailable(savedSeat, LocalDateTime.now()));
        return toResponse(savedSeat);
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
