package com.project.seat_reserve.outbox;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByAggregateTypeAndAggregateIdOrderByIdAsc(OutboxAggregateType aggregateType, Long aggregateId);
}
