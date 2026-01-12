package com.project.seat_reserve.order;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsBySessionId(String sessionId);
    boolean existsBySessionIdAndEventIdAndStatus(String sessionId, Long eventId, OrderStatus status);
    List<Order> findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(OrderStatus status, LocalDateTime createdAt, Pageable pageable);
    java.util.Optional<Order> findByIdAndStatus(Long id, OrderStatus status);
}
