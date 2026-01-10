package com.project.seat_reserve.projection;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatAvailabilityProjectionRepository extends JpaRepository<SeatAvailabilityProjection, Long> {
    List<SeatAvailabilityProjection> findByEventIdOrderBySectionAscRowLabelAscSeatNumberAsc(Long eventId);
}
