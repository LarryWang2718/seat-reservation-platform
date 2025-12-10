package com.project.seat_reserve.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.project.seat_reserve.event.dto.CreateEventRequest;
import com.project.seat_reserve.event.dto.EventResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;

    public EventResponse createEvent(CreateEventRequest request) {
        validateRequest(request);

        Event event = new Event();
        event.setName(request.getName());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setSaleStartTime(request.getSaleStartTime());
        event.setSaleEndTime(request.getSaleEndTime());
        event.setLocation(request.getLocation());
        event.setStatus(determineInitialStatus(request));
        return toResponse(eventRepository.save(event));
    }

    public List<EventResponse> getAllEvents() {
        List<Event> events = eventRepository.findAll();
        return events.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    private void validateRequest(CreateEventRequest request) {
        LocalDateTime now = LocalDateTime.now();

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("Event end time must be after start time");
        }

        if (!request.getSaleEndTime().isAfter(request.getSaleStartTime())) {
            throw new IllegalArgumentException("Sale end time must be after sale start time");
        }

        if (!request.getStartTime().isAfter(request.getSaleEndTime())) {
            throw new IllegalArgumentException("Sale end time cannot be after event start time");
        }

        if (!request.getSaleEndTime().isAfter(now)) {
            throw new IllegalArgumentException("Cannot create an event whose sale window has already ended");
        }
    }

    private EventStatus determineInitialStatus(CreateEventRequest request) {
        LocalDateTime now = LocalDateTime.now();

        if (!now.isBefore(request.getSaleStartTime()) && now.isBefore(request.getSaleEndTime())) {
            return EventStatus.ON_SALE;
        }

        return EventStatus.DRAFT;
    }

    private EventResponse toResponse(Event event) {
        return new EventResponse(
            event.getId(),
            event.getName(),
            event.getStartTime(),
            event.getEndTime(),
            event.getSaleStartTime(),
            event.getSaleEndTime(),
            event.getLocation(),
            event.getStatus()
        );
    }
}
