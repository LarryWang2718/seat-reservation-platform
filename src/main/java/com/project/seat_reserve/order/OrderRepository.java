package com.project.seat_reserve.order;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsBySessionId(String sessionId);
    boolean existsBySessionIdAndEventIdAndStatus(String sessionId, Long eventId, OrderStatus status);
}
