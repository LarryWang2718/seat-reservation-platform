package com.project.seat_reserve.ticket;

import java.time.LocalDateTime;

import com.project.seat_reserve.order.Order;
import com.project.seat_reserve.seat.Seat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ticket", uniqueConstraints = @UniqueConstraint(columnNames = {"seat_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Seat cannot be null")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @NotNull(message = "Order cannot be null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotNull(message = "Created at cannot be null")
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static Ticket createForOrder(Seat seat, Order order, LocalDateTime createdAt) {
        Ticket ticket = new Ticket();
        ticket.setSeat(seat);
        ticket.setOrder(order);
        ticket.setCreatedAt(createdAt);
        return ticket;
    }
}
