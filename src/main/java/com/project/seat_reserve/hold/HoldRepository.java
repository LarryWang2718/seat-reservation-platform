package com.project.seat_reserve.hold;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldRepository extends JpaRepository<Hold, Long> {
    List<Hold> findBySessionId(String sessionId);

    List<Hold> findBySeatId(Long seatId);

    List<Hold> findBySeatIdAndStatus(Long seatId, HoldStatus status);

    List<Hold> findBySessionIdAndStatus(String sessionId, HoldStatus status);

    List<Hold> findBySeatIdAndSessionId(Long seatId, String sessionId);

    List<Hold> findBySeatIdAndSessionIdAndStatus(Long seatId, String sessionId, HoldStatus status);
}
