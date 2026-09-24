package com.example.supportticket.ticket.api;

import com.example.supportticket.ticket.domain.TicketPriority;
import com.example.supportticket.ticket.domain.TicketStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TicketDetailResponse(
        UUID id,
        String title,
        String description,
        TicketPriority priority,
        TicketStatus status,
        String assignee,
        Instant createdAt,
        Instant updatedAt,
        List<CommentResponse> comments
) {
    public TicketDetailResponse {
        comments = List.copyOf(comments);
    }
}
