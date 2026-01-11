package com.project.seat_reserve.projection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserTicketProjectionServiceTest {
    @Mock
    private UserTicketProjectionRepository userTicketProjectionRepository;

    @InjectMocks
    private UserTicketProjectionService userTicketProjectionService;

    @Test
    void getTicketsBySessionReturnsProjectionRowsInRepositoryOrder() {
        UserTicketProjection firstTicket = new UserTicketProjection(
            1L, 10L, 100L, "session-123", 1000L, "A", "1", "10",
            LocalDateTime.of(2026, 3, 17, 12, 0), LocalDateTime.of(2026, 3, 17, 12, 0)
        );
        UserTicketProjection secondTicket = new UserTicketProjection(
            2L, 11L, 100L, "session-123", 1001L, "A", "1", "11",
            LocalDateTime.of(2026, 3, 17, 12, 1), LocalDateTime.of(2026, 3, 17, 12, 1)
        );

        when(userTicketProjectionRepository.findBySessionIdOrderByTicketIdAsc("session-123"))
            .thenReturn(List.of(firstTicket, secondTicket));

        List<UserTicketProjection> tickets = userTicketProjectionService.getTicketsBySession("session-123");

        assertEquals(2, tickets.size());
        assertEquals(1L, tickets.get(0).getTicketId());
        assertEquals(2L, tickets.get(1).getTicketId());
        verify(userTicketProjectionRepository).findBySessionIdOrderByTicketIdAsc("session-123");
    }
}
