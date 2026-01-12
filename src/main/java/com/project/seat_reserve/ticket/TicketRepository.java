package com.project.seat_reserve.ticket;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    boolean existsBySeatId(Long seatId);

    List<Ticket> findByOrderIdIn(List<Long> orderIds);
}
