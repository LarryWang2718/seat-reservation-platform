package com.project.seat_reserve.projection;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserTicketProjectionService {
    private final UserTicketProjectionRepository userTicketProjectionRepository;

    public List<UserTicketProjection> getTicketsBySession(String sessionId) {
        return userTicketProjectionRepository.findBySessionIdOrderByTicketIdAsc(sessionId);
    }
}
