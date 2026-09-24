package com.example.supportticket.ticket.api;

import java.util.List;

public record TicketPageResponse(
        List<TicketSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public TicketPageResponse {
        content = List.copyOf(content);
    }
}
