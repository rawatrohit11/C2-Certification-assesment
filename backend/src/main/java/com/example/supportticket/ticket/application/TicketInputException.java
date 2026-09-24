package com.example.supportticket.ticket.application;

import com.example.supportticket.shared.error.FieldErrorDetail;

import java.util.List;

public class TicketInputException extends RuntimeException {

    private final List<FieldErrorDetail> fieldErrors;

    public TicketInputException(String message, List<FieldErrorDetail> fieldErrors) {
        super(message);
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<FieldErrorDetail> getFieldErrors() {
        return fieldErrors;
    }
}
