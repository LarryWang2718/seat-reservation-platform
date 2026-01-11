package com.project.seat_reserve.projection;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectionEventFailureRepository extends JpaRepository<ProjectionEventFailure, Long> {
    Optional<ProjectionEventFailure> findByConsumerNameAndOutboxEventId(String consumerName, Long outboxEventId);

    void deleteByConsumerNameAndOutboxEventId(String consumerName, Long outboxEventId);
}
