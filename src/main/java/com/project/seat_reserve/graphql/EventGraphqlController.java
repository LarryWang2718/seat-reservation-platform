package com.project.seat_reserve.graphql;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.project.seat_reserve.event.EventService;
import com.project.seat_reserve.event.dto.CreateEventRequest;
import com.project.seat_reserve.event.dto.EventResponse;
import com.project.seat_reserve.graphql.dto.CreateEventInput;
import com.project.seat_reserve.graphql.dto.EventView;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class EventGraphqlController {
    private final EventService eventService;

    @QueryMapping
    public List<EventView> events() {
        return eventService.getAllEvents().stream()
            .map(this::toView)
            .toList();
    }

    @MutationMapping
    public EventView createEvent(@Argument("input") CreateEventInput input) {
        CreateEventRequest request = new CreateEventRequest(
            input.name(),
            LocalDateTime.parse(input.startTime()),
            LocalDateTime.parse(input.endTime()),
            LocalDateTime.parse(input.saleStartTime()),
            LocalDateTime.parse(input.saleEndTime()),
            input.location()
        );

        return toView(eventService.createEvent(request));
    }

    private EventView toView(EventResponse response) {
        return new EventView(
            response.getId(),
            response.getName(),
            response.getStartTime().toString(),
            response.getEndTime().toString(),
            response.getSaleStartTime().toString(),
            response.getSaleEndTime().toString(),
            response.getLocation(),
            response.getStatus().name()
        );
    }
}
