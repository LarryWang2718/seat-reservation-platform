package com.project.seat_reserve.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.seat_reserve.event.dto.CreateEventRequest;
import com.project.seat_reserve.event.dto.EventResponse;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {
    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    @Captor
    private ArgumentCaptor<Event> eventCaptor;

    @Test
    void createEventSetsDraftWhenSaleHasNotStarted() {
        CreateEventRequest request = new CreateEventRequest();
        request.setName("Test Event");
        request.setStartTime(LocalDateTime.now().plusDays(2));
        request.setEndTime(LocalDateTime.now().plusDays(2).plusHours(2));
        request.setSaleStartTime(LocalDateTime.now().plusDays(1));
        request.setSaleEndTime(LocalDateTime.now().plusDays(1).plusHours(4));
        request.setLocation("Test Location");

        Event savedEvent = new Event();
        savedEvent.setId(1L);
        savedEvent.setName(request.getName());
        savedEvent.setStartTime(request.getStartTime());
        savedEvent.setEndTime(request.getEndTime());
        savedEvent.setSaleStartTime(request.getSaleStartTime());
        savedEvent.setSaleEndTime(request.getSaleEndTime());
        savedEvent.setLocation(request.getLocation());
        savedEvent.setStatus(EventStatus.DRAFT);

        when(eventRepository.save(eventCaptor.capture())).thenReturn(savedEvent);

        EventResponse response = eventService.createEvent(request);
        Event capturedEvent = eventCaptor.getValue();

        assertEquals("Test Event", capturedEvent.getName());
        assertEquals(request.getStartTime(), capturedEvent.getStartTime());
        assertEquals(request.getEndTime(), capturedEvent.getEndTime());
        assertEquals(request.getSaleStartTime(), capturedEvent.getSaleStartTime());
        assertEquals(request.getSaleEndTime(), capturedEvent.getSaleEndTime());
        assertEquals("Test Location", capturedEvent.getLocation());
        assertEquals(EventStatus.DRAFT, capturedEvent.getStatus());

        assertEquals("Test Event", response.getName());
        assertEquals("Test Location", response.getLocation());
        assertEquals(EventStatus.DRAFT, response.getStatus());
        assertEquals(request.getSaleStartTime(), response.getSaleStartTime());
        assertEquals(request.getSaleEndTime(), response.getSaleEndTime());
    }

    @Test
    void createEventSetsOnSaleWhenSaleWindowIsOpen() {
        CreateEventRequest request = new CreateEventRequest();
        request.setName("On Sale Event");
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        request.setSaleStartTime(LocalDateTime.now().minusHours(1));
        request.setSaleEndTime(LocalDateTime.now().plusHours(3));
        request.setLocation("Arena");

        Event savedEvent = new Event();
        savedEvent.setId(2L);
        savedEvent.setName(request.getName());
        savedEvent.setStartTime(request.getStartTime());
        savedEvent.setEndTime(request.getEndTime());
        savedEvent.setSaleStartTime(request.getSaleStartTime());
        savedEvent.setSaleEndTime(request.getSaleEndTime());
        savedEvent.setLocation(request.getLocation());
        savedEvent.setStatus(EventStatus.ON_SALE);

        when(eventRepository.save(eventCaptor.capture())).thenReturn(savedEvent);

        EventResponse response = eventService.createEvent(request);
        Event capturedEvent = eventCaptor.getValue();

        assertEquals(EventStatus.ON_SALE, capturedEvent.getStatus());
        assertEquals(EventStatus.ON_SALE, response.getStatus());
    }

    @Test
    void createEventRejectsInvalidSaleWindow() {
        CreateEventRequest request = new CreateEventRequest();
        request.setName("Broken Event");
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        request.setSaleStartTime(LocalDateTime.now().plusHours(3));
        request.setSaleEndTime(LocalDateTime.now().plusHours(1));
        request.setLocation("Arena");

        assertThrows(IllegalArgumentException.class, () -> eventService.createEvent(request));
    }

    @Test
    void createEventRejectsWhenSaleWindowAlreadyEnded() {
        CreateEventRequest request = new CreateEventRequest();
        request.setName("Closed Sale Event");
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        request.setSaleStartTime(LocalDateTime.now().minusDays(2));
        request.setSaleEndTime(LocalDateTime.now().minusHours(1));
        request.setLocation("Arena");

        assertThrows(IllegalArgumentException.class, () -> eventService.createEvent(request));
    }
}
