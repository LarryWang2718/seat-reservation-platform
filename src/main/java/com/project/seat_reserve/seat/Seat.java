package com.project.seat_reserve.seat;

import com.project.seat_reserve.event.Event;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "seat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @NotNull(message = "Event is required")
    private Event event;

    @NotBlank(message = "Section is required")
    private String section;

    @NotBlank(message = "Row is required")
    private String rowLabel;

    @NotBlank(message = "Seat number is required")
    private String seatNumber;

    public static Seat create(Event event, String section, String rowLabel, String seatNumber) {
        Seat seat = new Seat();
        seat.setEvent(event);
        seat.setSection(section);
        seat.setRowLabel(rowLabel);
        seat.setSeatNumber(seatNumber);
        return seat;
    }
}
