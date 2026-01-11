package com.project.seat_reserve.graphql;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.project.seat_reserve.graphql.dto.UserTicketView;
import com.project.seat_reserve.projection.UserTicketProjection;
import com.project.seat_reserve.projection.UserTicketProjectionService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class UserTicketGraphqlController {
    private final UserTicketProjectionService userTicketProjectionService;

    @QueryMapping
    public List<UserTicketView> ticketsBySession(@Argument String sessionId) {
        return userTicketProjectionService.getTicketsBySession(sessionId).stream()
            .map(this::toView)
            .toList();
    }

    private UserTicketView toView(UserTicketProjection projection) {
        return new UserTicketView(
            projection.getTicketId(),
            projection.getOrderId(),
            projection.getEventId(),
            projection.getSessionId(),
            projection.getSeatId(),
            projection.getSection(),
            projection.getRowLabel(),
            projection.getSeatNumber(),
            projection.getIssuedAt().toString()
        );
    }
}
