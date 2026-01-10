package com.project.seat_reserve.projection;

import java.time.LocalDateTime;

import com.project.seat_reserve.seat.Seat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "seat_availability_projection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeatAvailabilityProjection {
    @Id
    @Column(name = "seat_id")
    private Long seatId;

    @NotNull
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @NotNull
    @Column(name = "section", nullable = false)
    private String section;

    @NotNull
    @Column(name = "row_label", nullable = false)
    private String rowLabel;

    @NotNull
    @Column(name = "seat_number", nullable = false)
    private String seatNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SeatAvailabilityStatus status;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "hold_id")
    private Long holdId;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @Column(name = "ticket_id")
    private Long ticketId;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static SeatAvailabilityProjection createAvailable(Seat seat, LocalDateTime updatedAt) {
        SeatAvailabilityProjection projection = new SeatAvailabilityProjection();
        projection.setSeatId(seat.getId());
        projection.setEventId(seat.getEvent().getId());
        projection.setSection(seat.getSection());
        projection.setRowLabel(seat.getRowLabel());
        projection.setSeatNumber(seat.getSeatNumber());
        projection.setStatus(SeatAvailabilityStatus.AVAILABLE);
        projection.setUpdatedAt(updatedAt);
        return projection;
    }

    public void markHeld(Long orderId, Long holdId, String sessionId, LocalDateTime holdExpiresAt, LocalDateTime updatedAt) {
        this.status = SeatAvailabilityStatus.HELD;
        this.orderId = orderId;
        this.holdId = holdId;
        this.sessionId = sessionId;
        this.holdExpiresAt = holdExpiresAt;
        this.ticketId = null;
        this.updatedAt = updatedAt;
    }

    public void markSold(Long orderId, Long ticketId, String sessionId, LocalDateTime updatedAt) {
        this.status = SeatAvailabilityStatus.SOLD;
        this.orderId = orderId;
        this.holdId = null;
        this.sessionId = sessionId;
        this.holdExpiresAt = null;
        this.ticketId = ticketId;
        this.updatedAt = updatedAt;
    }
}
