package com.example.supportticket.ticket.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddCommentRequest(
        @NotBlank @Size(max = 2000) String body
) {
    public AddCommentRequest {
        body = body == null ? null : body.trim();
    }
}
