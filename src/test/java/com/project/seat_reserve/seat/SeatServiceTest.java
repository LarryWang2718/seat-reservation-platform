package com.project.seat_reserve.seat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.seat_reserve.event.Event;
import com.project.seat_reserve.event.EventRepository;
import com.project.seat_reserve.event.EventStatus;
import com.project.seat_reserve.seat.dto.CreateSeatRequest;
import com.project.seat_reserve.seat.dto.SeatResponse;

@ExtendWith(MockitoExtension.class)
class SeatServiceTest {
    @Mock
    private EventRepository eventRepository;

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private SeatService seatService;

    @Captor
    private ArgumentCaptor<Seat> seatCaptor;

    @Test
    void createSeatBuildsSeatFromRequest() {
        Event event = new Event();
        event.setId(1L);
        event.setName("Concert");
        event.setStartTime(LocalDateTime.now().plusDays(1));
        event.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        event.setSaleStartTime(LocalDateTime.now().minusHours(1));
        event.setSaleEndTime(LocalDateTime.now().plusHours(6));
        event.setLocation("Arena");
        event.setStatus(EventStatus.ON_SALE);

        CreateSeatRequest request = new CreateSeatRequest(1L, "A", "1", "10");

        Seat savedSeat = new Seat();
        savedSeat.setId(100L);
        savedSeat.setEvent(event);
        savedSeat.setSection("A");
        savedSeat.setRowLabel("1");
        savedSeat.setSeatNumber("10");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(seatRepository.save(seatCaptor.capture())).thenReturn(savedSeat);

        SeatResponse response = seatService.createSeat(request);
        Seat capturedSeat = seatCaptor.getValue();

        assertEquals(event, capturedSeat.getEvent());
        assertEquals("A", capturedSeat.getSection());
        assertEquals("1", capturedSeat.getRowLabel());
        assertEquals("10", capturedSeat.getSeatNumber());

        assertEquals(100L, response.getId());
        assertEquals(1L, response.getEventId());
        assertEquals("A", response.getSection());
        assertEquals("1", response.getRowLabel());
        assertEquals("10", response.getSeatNumber());
    }

    @Test
    void createSeatRejectsMissingEvent() {
        CreateSeatRequest request = new CreateSeatRequest(99L, "A", "1", "10");

        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> seatService.createSeat(request));
    }

    @Test
    void getSeatsByEventIdMapsRepositoryResults() {
        Event event = new Event();
        event.setId(5L);

        Seat firstSeat = new Seat(1L, event, "A", "1", "1");
        Seat secondSeat = new Seat(2L, event, "A", "1", "2");

        when(seatRepository.findByEventId(5L)).thenReturn(List.of(firstSeat, secondSeat));

        List<SeatResponse> response = seatService.getSeatsByEventId(5L);

        assertEquals(2, response.size());
        assertEquals("1", response.get(0).getSeatNumber());
        assertEquals("2", response.get(1).getSeatNumber());
    }
}
