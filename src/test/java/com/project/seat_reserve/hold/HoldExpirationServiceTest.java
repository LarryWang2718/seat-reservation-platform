package com.project.seat_reserve.hold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.project.seat_reserve.event.Event;
import com.project.seat_reserve.order.Order;
import com.project.seat_reserve.order.OrderStatus;
import com.project.seat_reserve.outbox.OutboxEventService;
import com.project.seat_reserve.seat.Seat;

@ExtendWith(MockitoExtension.class)
class HoldExpirationServiceTest {
    @Mock
    private HoldRepository holdRepository;

    @Mock
    private OutboxEventService outboxEventService;

    @InjectMocks
    private HoldExpirationService holdExpirationService;

    @Captor
    private ArgumentCaptor<List<Hold>> holdsCaptor;

    @Captor
    private ArgumentCaptor<LocalDateTime> expiredAtCaptor;

    @Test
    void expireStaleHoldsMarksHeldRowsExpiredAndPublishesOutboxEvents() {
        Hold firstHold = buildHold(1L, 10L, 100L, HoldStatus.HELD, LocalDateTime.now().minusMinutes(2));
        Hold secondHold = buildHold(2L, 11L, 101L, HoldStatus.HELD, LocalDateTime.now().minusMinutes(1));

        when(holdRepository.findByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(any(HoldStatus.class), any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(List.of(firstHold, secondHold));

        int expiredCount = holdExpirationService.expireStaleHolds(50);

        assertEquals(2, expiredCount);
        assertEquals(HoldStatus.EXPIRED, firstHold.getStatus());
        assertEquals(HoldStatus.EXPIRED, secondHold.getStatus());
        verify(outboxEventService, times(2)).publishHoldExpired(any(Hold.class), expiredAtCaptor.capture());
        assertEquals(2, expiredAtCaptor.getAllValues().size());
        assertEquals(expiredAtCaptor.getAllValues().get(0), expiredAtCaptor.getAllValues().get(1));
        assertTrue(expiredAtCaptor.getAllValues().get(0).isAfter(secondHold.getExpiresAt()));
        verify(holdRepository).saveAll(holdsCaptor.capture());
        assertEquals(2, holdsCaptor.getValue().size());
        assertEquals(firstHold, holdsCaptor.getValue().get(0));
        assertEquals(secondHold, holdsCaptor.getValue().get(1));
    }

    @Test
    void expireStaleHoldsDoesNothingWhenNoRowsNeedExpiration() {
        when(holdRepository.findByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(any(HoldStatus.class), any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(List.of());

        int expiredCount = holdExpirationService.expireStaleHolds(25);

        assertEquals(0, expiredCount);
        verify(outboxEventService, never()).publishHoldExpired(any(Hold.class), any(LocalDateTime.class));
        verify(holdRepository, never()).saveAll(any());
    }

    private Hold buildHold(Long holdId, Long orderId, Long seatId, HoldStatus status, LocalDateTime expiresAt) {
        Event event = new Event();
        event.setId(200L);

        Order order = new Order();
        order.setId(orderId);
        order.setEvent(event);
        order.setSessionId("session-" + orderId);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        Seat seat = new Seat();
        seat.setId(seatId);
        seat.setEvent(event);
        seat.setSection("A");
        seat.setRowLabel("1");
        seat.setSeatNumber(String.valueOf(seatId));

        Hold hold = new Hold();
        hold.setId(holdId);
        hold.setOrder(order);
        hold.setSeat(seat);
        hold.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        hold.setExpiresAt(expiresAt);
        hold.setStatus(status);
        return hold;
    }
}
