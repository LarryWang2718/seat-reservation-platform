package com.project.seat_reserve.projection;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectionCheckpointRepository extends JpaRepository<ProjectionCheckpoint, String> {
}
