package com.project.seat_reserve.order;

import java.time.LocalDateTime;

import com.project.seat_reserve.event.Event;

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
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Event cannot be null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @NotNull(message = "Session ID cannot be null")
    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @NotNull(message = "Status cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @NotNull(message = "Created at cannot be null")
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static Order createPending(Event event, String sessionId, LocalDateTime createdAt) {
        Order order = new Order();
        order.setEvent(event);
        order.setSessionId(sessionId);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(createdAt);
        return order;
    }

    public void markCompleted() {
        this.status = OrderStatus.COMPLETED;
    }

    public void markCancelled() {
        this.status = OrderStatus.CANCELLED;
    }
}
