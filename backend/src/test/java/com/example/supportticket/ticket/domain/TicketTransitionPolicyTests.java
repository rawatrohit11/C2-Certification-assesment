package com.example.supportticket.ticket.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TicketTransitionPolicyTests {

    private static final Set<Transition> ALLOWED = Set.of(
            new Transition(TicketStatus.OPEN, TicketStatus.IN_PROGRESS),
            new Transition(TicketStatus.OPEN, TicketStatus.CANCELLED),
            new Transition(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED),
            new Transition(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
            new Transition(TicketStatus.RESOLVED, TicketStatus.CLOSED)
    );

    private final TicketTransitionPolicy policy = new TicketTransitionPolicy();

    @Test
    void allowsExactlyTheApprovedTransitionMatrix() {
        for (TicketStatus current : TicketStatus.values()) {
            for (TicketStatus target : TicketStatus.values()) {
                assertThat(policy.canTransition(current, target))
                        .as("%s -> %s", current, target)
                        .isEqualTo(ALLOWED.contains(new Transition(current, target)));
            }
        }
    }

    @Test
    void rejectsNullStatuses() {
        assertThat(policy.canTransition(null, TicketStatus.OPEN)).isFalse();
        assertThat(policy.canTransition(TicketStatus.OPEN, null)).isFalse();
    }

    private record Transition(TicketStatus current, TicketStatus target) {
    }
}
