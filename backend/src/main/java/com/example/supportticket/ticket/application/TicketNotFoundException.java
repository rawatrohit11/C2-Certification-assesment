package com.example.supportticket.ticket.application;

import java.util.UUID;

public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(UUID ticketId) {
        super("Ticket " + ticketId + " was not found.");
    }
}
