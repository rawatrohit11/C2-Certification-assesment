package com.example.supportticket.ticket.domain;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class TicketTransitionPolicy {

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED_TRANSITIONS = Map.of(
            TicketStatus.OPEN, Set.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
            TicketStatus.IN_PROGRESS, Set.of(TicketStatus.RESOLVED, TicketStatus.CANCELLED),
            TicketStatus.RESOLVED, Set.of(TicketStatus.CLOSED),
            TicketStatus.CLOSED, Set.of(),
            TicketStatus.CANCELLED, Set.of()
    );

    public boolean canTransition(TicketStatus currentStatus, TicketStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(targetStatus);
    }
}
