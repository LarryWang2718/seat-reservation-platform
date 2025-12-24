package com.project.seat_reserve.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.project.seat_reserve.common.exception.EventSaleWindowClosedException;
import com.project.seat_reserve.common.exception.InvalidEventTimeRangeException;
import com.project.seat_reserve.common.exception.InvalidSaleWindowException;
import com.project.seat_reserve.common.exception.SaleWindowAfterEventStartException;
import com.project.seat_reserve.event.dto.CreateEventRequest;
import com.project.seat_reserve.event.dto.EventResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;

    public EventResponse createEvent(CreateEventRequest request) {
        validateEventRequest(request);

        Event event = Event.create(
            request.getName(),
            request.getStartTime(),
            request.getEndTime(),
            request.getSaleStartTime(),
            request.getSaleEndTime(),
            request.getLocation(),
            determineInitialStatus(request)
        );
        return toResponse(eventRepository.save(event));
    }

    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    private void validateEventRequest(CreateEventRequest request) {
        LocalDateTime now = LocalDateTime.now();

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new InvalidEventTimeRangeException();
        }

        if (!request.getSaleEndTime().isAfter(request.getSaleStartTime())) {
            throw new InvalidSaleWindowException();
        }

        if (!request.getStartTime().isAfter(request.getSaleEndTime())) {
            throw new SaleWindowAfterEventStartException();
        }

        if (!request.getSaleEndTime().isAfter(now)) {
            throw new EventSaleWindowClosedException();
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
