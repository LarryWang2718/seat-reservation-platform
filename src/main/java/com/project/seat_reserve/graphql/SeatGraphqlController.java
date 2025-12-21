package com.project.seat_reserve.graphql;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import com.project.seat_reserve.graphql.dto.CreateSeatInput;
import com.project.seat_reserve.seat.SeatService;
import com.project.seat_reserve.seat.dto.CreateSeatRequest;
import com.project.seat_reserve.seat.dto.SeatResponse;

import lombok.RequiredArgsConstructor;

@Controller
@Validated
@RequiredArgsConstructor
public class SeatGraphqlController {
    private final SeatService seatService;

    @QueryMapping
    public List<SeatResponse> seatsByEvent(@Argument Long eventId) {
        return seatService.getSeatsByEventId(eventId);
    }

    @MutationMapping
    public SeatResponse createSeat(@Argument("input") @Valid CreateSeatInput input) {
        CreateSeatRequest request = new CreateSeatRequest(
            input.eventId(),
            input.section(),
            input.rowLabel(),
            input.seatNumber()
        );

        return seatService.createSeat(request);
    }
}
