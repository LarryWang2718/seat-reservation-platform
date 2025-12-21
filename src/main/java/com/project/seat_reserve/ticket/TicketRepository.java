package com.project.seat_reserve.ticket;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    boolean existsBySeatId(Long seatId);
}
