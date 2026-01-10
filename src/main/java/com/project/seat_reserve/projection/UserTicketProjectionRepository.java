package com.project.seat_reserve.projection;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTicketProjectionRepository extends JpaRepository<UserTicketProjection, Long> {
    List<UserTicketProjection> findBySessionIdOrderByTicketIdAsc(String sessionId);
}
