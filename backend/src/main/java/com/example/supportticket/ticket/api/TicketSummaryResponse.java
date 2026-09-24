package com.example.supportticket.ticket.api;

import com.example.supportticket.ticket.domain.TicketPriority;
import com.example.supportticket.ticket.domain.TicketStatus;

import java.time.Instant;
import java.util.UUID;

public record TicketSummaryResponse(
        UUID id,
        String title,
        TicketPriority priority,
        TicketStatus status,
        String assignee,
        Instant createdAt,
        Instant updatedAt
) {
}
