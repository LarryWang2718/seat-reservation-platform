package com.project.seat_reserve.event;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "event")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name cannot be blank")
    @NotNull(message = "Name cannot be null")
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull(message = "Start time cannot be null")
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @NotNull(message = "End time cannot be null")
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @NotNull(message = "Sale start time cannot be null")
    @Column(name = "sale_start_time", nullable = false)
    private LocalDateTime saleStartTime;

    @NotNull(message = "Sale end time cannot be null")
    @Column(name = "sale_end_time", nullable = false)
    private LocalDateTime saleEndTime;

    @NotBlank(message = "Location cannot be blank")
    @NotNull(message = "Location cannot be null")
    @Column(name = "location", nullable = false)
    private String location;

    @NotNull(message = "Status cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EventStatus status;

    public static Event create(String name, LocalDateTime startTime, LocalDateTime endTime,
                               LocalDateTime saleStartTime, LocalDateTime saleEndTime,
                               String location, EventStatus status) {
        Event event = new Event();
        event.setName(name);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setSaleStartTime(saleStartTime);
        event.setSaleEndTime(saleEndTime);
        event.setLocation(location);
        event.setStatus(status);
        return event;
    }
}
