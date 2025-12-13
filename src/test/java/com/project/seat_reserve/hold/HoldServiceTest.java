package com.project.seat_reserve.hold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.project.seat_reserve.hold.dto.CreateHoldRequest;
import com.project.seat_reserve.hold.dto.HoldResponse;
import com.project.seat_reserve.seat.Seat;
import com.project.seat_reserve.seat.SeatRepository;

@ExtendWith(MockitoExtension.class)
class HoldServiceTest {
    @Mock
    private SeatRepository seatRepository;

    @Mock
    private HoldRepository holdRepository;

    @InjectMocks
    private HoldService holdService;

    @Captor
    private ArgumentCaptor<Hold> holdCaptor;

    @Test
    void createHoldBuildsHeldRecordFromRequest() {
        Event event = new Event();
        event.setId(1L);

        Seat seat = new Seat();
        seat.setId(10L);
        seat.setEvent(event);
        seat.setSection("A");
        seat.setRowLabel("1");
        seat.setSeatNumber("5");

        CreateHoldRequest request = new CreateHoldRequest(10L, "session-123");

        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime expiresAt = createdAt.plusMinutes(5);

        Hold savedHold = new Hold();
        savedHold.setId(100L);
        savedHold.setSeat(seat);
        savedHold.setSessionId("session-123");
        savedHold.setCreatedAt(createdAt);
        savedHold.setExpiresAt(expiresAt);
        savedHold.setStatus(HoldStatus.HELD);

        when(seatRepository.findById(10L)).thenReturn(Optional.of(seat));
        when(holdRepository.save(holdCaptor.capture())).thenReturn(savedHold);

        HoldResponse response = holdService.createHold(request);
        Hold capturedHold = holdCaptor.getValue();

        assertEquals(seat, capturedHold.getSeat());
        assertEquals("session-123", capturedHold.getSessionId());
        assertEquals(HoldStatus.HELD, capturedHold.getStatus());
        assertEquals(capturedHold.getCreatedAt().plusMinutes(5), capturedHold.getExpiresAt());

        assertEquals(100L, response.getId());
        assertEquals(10L, response.getSeatId());
        assertEquals("session-123", response.getSessionId());
        assertEquals(HoldStatus.HELD, response.getStatus());
        assertEquals(createdAt, response.getCreatedAt());
        assertEquals(expiresAt, response.getExpiresAt());
    }

    @Test
    void createHoldRejectsMissingSeat() {
        CreateHoldRequest request = new CreateHoldRequest(99L, "session-123");

        when(seatRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> holdService.createHold(request));
    }

    @Test
    void getHoldsBySessionIdMapsRepositoryResults() {
        Event event = new Event();
        event.setId(1L);

        Seat seat = new Seat();
        seat.setId(10L);
        seat.setEvent(event);

        Hold firstHold = new Hold(
            1L,
            seat,
            "session-123",
            LocalDateTime.of(2025, 12, 10, 17, 25, 50),
            LocalDateTime.of(2025, 12, 10, 17, 20, 50),
            HoldStatus.HELD
        );

        Hold secondHold = new Hold(
            2L,
            seat,
            "session-123",
            LocalDateTime.of(2025, 12, 10, 17, 30, 50),
            LocalDateTime.of(2025, 12, 10, 17, 25, 50),
            HoldStatus.EXPIRED
        );

        when(holdRepository.findBySessionId("session-123")).thenReturn(List.of(firstHold, secondHold));

        List<HoldResponse> response = holdService.getHoldsBySessionId("session-123");

        assertEquals(2, response.size());
        assertEquals(1L, response.get(0).getId());
        assertEquals(HoldStatus.HELD, response.get(0).getStatus());
        assertEquals(2L, response.get(1).getId());
        assertEquals(HoldStatus.EXPIRED, response.get(1).getStatus());
    }
}
