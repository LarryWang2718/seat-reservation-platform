package com.project.seat_reserve.hold;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldRepository extends JpaRepository<Hold, Long> {
    List<Hold> findByOrderId(Long orderId);

    long countByOrderIdAndStatus(Long orderId, HoldStatus status);

    boolean existsBySeatIdAndStatus(Long seatId, HoldStatus status);

    boolean existsBySeatIdAndOrderIdAndStatus(Long seatId, Long orderId, HoldStatus status);
}
