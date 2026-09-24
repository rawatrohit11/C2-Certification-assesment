package com.example.supportticket.ticket.api;

import com.example.supportticket.ticket.domain.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 5000) String description,
        @NotNull TicketPriority priority,
        @Size(max = 100) String assignee
) {
    public CreateTicketRequest {
        title = trim(title);
        description = trim(description);
        assignee = normalizeOptional(assignee);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeOptional(String value) {
        String normalized = trim(value);
        return normalized == null || normalized.isEmpty() ? null : normalized;
    }
}
