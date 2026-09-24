package com.example.supportticket.ticket.api;

import jakarta.validation.constraints.NotBlank;

public record TransitionTicketRequest(
        @NotBlank String targetStatus
) {
    public TransitionTicketRequest {
        targetStatus = targetStatus == null ? null : targetStatus.trim();
    }
}
