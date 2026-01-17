package com.project.seat_reserve.outbox;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByAggregateTypeAndAggregateIdOrderByIdAsc(OutboxAggregateType aggregateType, Long aggregateId);

    List<OutboxEvent> findByIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);

    Optional<OutboxEvent> findTopByOrderByIdDesc();
}
