package com.project.seat_reserve.projection;

import java.time.LocalDateTime;

import com.project.seat_reserve.outbox.TicketIssuedPayload;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_ticket_projection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserTicketProjection {
    @Id
    @Column(name = "ticket_id")
    private Long ticketId;

    @NotNull
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @NotNull
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @NotNull
    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @NotNull
    @Column(name = "seat_id", nullable = false)
    private Long seatId;

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
    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static UserTicketProjection fromPayload(TicketIssuedPayload payload, LocalDateTime updatedAt) {
        UserTicketProjection projection = new UserTicketProjection();
        projection.apply(payload, updatedAt);
        return projection;
    }

    public void apply(TicketIssuedPayload payload, LocalDateTime updatedAt) {
        this.ticketId = payload.ticketId();
        this.orderId = payload.orderId();
        this.eventId = payload.eventId();
        this.sessionId = payload.sessionId();
        this.seatId = payload.seatId();
        this.section = payload.section();
        this.rowLabel = payload.rowLabel();
        this.seatNumber = payload.seatNumber();
        this.issuedAt = payload.issuedAt();
        this.updatedAt = updatedAt;
    }
}
