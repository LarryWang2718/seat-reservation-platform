package com.project.seat_reserve.projection;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "projection_checkpoint")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectionCheckpoint {
    @Id
    @Column(name = "consumer_name")
    private String consumerName;

    @NotNull
    @Column(name = "last_processed_event_id", nullable = false)
    private Long lastProcessedEventId;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static ProjectionCheckpoint initialize(@NotBlank String consumerName, LocalDateTime updatedAt) {
        return new ProjectionCheckpoint(consumerName, 0L, updatedAt);
    }

    public void advanceTo(Long eventId, LocalDateTime updatedAt) {
        this.lastProcessedEventId = eventId;
        this.updatedAt = updatedAt;
    }
}
