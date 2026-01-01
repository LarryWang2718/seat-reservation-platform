package com.project.seat_reserve.graphql;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import com.project.seat_reserve.graphql.dto.CreateHoldInput;
import com.project.seat_reserve.graphql.dto.HoldView;
import com.project.seat_reserve.hold.HoldService;
import com.project.seat_reserve.hold.dto.CreateHoldRequest;
import com.project.seat_reserve.hold.dto.HoldResponse;

import lombok.RequiredArgsConstructor;

@Controller
@Validated
@RequiredArgsConstructor
public class HoldGraphqlController {
    private final HoldService holdService;

    @QueryMapping
    public List<HoldView> holdsByOrder(@Argument Long orderId) {
        return holdService.getHoldsByOrderId(orderId).stream()
            .map(this::toView)
            .toList();
    }

    @MutationMapping
    public HoldView createHold(@Argument("input") @Valid CreateHoldInput input) {
        CreateHoldRequest request = new CreateHoldRequest(input.seatId(), input.orderId());
        return toView(holdService.createHold(request));
    }

    private HoldView toView(HoldResponse response) {
        return new HoldView(
            response.getId(),
            response.getSeatId(),
            response.getOrderId(),
            response.getExpiresAt().toString(),
            response.getCreatedAt().toString(),
            response.getStatus().name()
        );
    }
}
