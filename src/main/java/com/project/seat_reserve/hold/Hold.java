package com.project.seat_reserve.hold;

import java.time.LocalDateTime;

import com.project.seat_reserve.order.Order;
import com.project.seat_reserve.seat.Seat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hold")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Hold {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Seat cannot be null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @NotNull(message = "Order cannot be null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotNull(message = "Expires at cannot be null")
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @NotNull(message = "Created at cannot be null")
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @NotNull(message = "Status cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private HoldStatus status;

    public static Hold createHeld(Order order, Seat seat, LocalDateTime createdAt, LocalDateTime expiresAt) {
        Hold hold = new Hold();
        hold.setOrder(order);
        hold.setSeat(seat);
        hold.setCreatedAt(createdAt);
        hold.setExpiresAt(expiresAt);
        hold.setStatus(HoldStatus.HELD);
        return hold;
    }

    public void markConfirmed() {
        this.status = HoldStatus.CONFIRMED;
    }

    public void markExpired() {
        this.status = HoldStatus.EXPIRED;
    }

    public void markCancelled() {
        this.status = HoldStatus.CANCELLED;
    }
}
