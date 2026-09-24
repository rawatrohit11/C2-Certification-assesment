package com.example.supportticket.ticket.application;

import com.example.supportticket.ticket.domain.TicketStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(
            TicketStatus currentStatus,
            TicketStatus targetStatus
    ) {
        super("Ticket cannot transition from " + currentStatus + " to " + targetStatus + ".");
    }
}
