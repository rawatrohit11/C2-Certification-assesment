package com.example.supportticket.ticket.api;

import com.example.supportticket.ticket.domain.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TicketTransitionApiIntegrationTests {

    private static final OffsetDateTime BASELINE_UPDATED_AT =
            OffsetDateTime.parse("2000-01-01T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearDatabase() {
        jdbcTemplate.update("DELETE FROM ticket_comments");
        jdbcTemplate.update("DELETE FROM tickets");
    }

    @ParameterizedTest(name = "{0} transitions to {1}")
    @MethodSource("allowedTransitions")
    void persistsAllowedTransitions(TicketStatus current, TicketStatus target) throws Exception {
        UUID ticketId = createTicket();
        setStatus(ticketId, current);

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/transitions", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetStatus": "%s"}
                                """.formatted(target)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(target.name()));

        assertThat(persistedStatus(ticketId)).isEqualTo(target);
        assertThat(persistedUpdatedAt(ticketId)).isAfter(BASELINE_UPDATED_AT);
    }

    @ParameterizedTest(name = "{0} rejects {1}")
    @MethodSource("representativeInvalidTransitions")
    void rejectsInvalidTransitionsWithoutChangingStatus(
            TicketStatus current,
            TicketStatus target
    ) throws Exception {
        UUID ticketId = createTicket();
        setStatus(ticketId, current);

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/transitions", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetStatus": "%s"}
                                """.formatted(target)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"))
                .andExpect(jsonPath("$.detail").value(
                        "Ticket cannot transition from " + current + " to " + target + "."
                ));

        assertThat(persistedStatus(ticketId)).isEqualTo(current);
        assertThat(persistedUpdatedAt(ticketId)).isEqualTo(BASELINE_UPDATED_AT);
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"CLOSED", "CANCELLED"})
    void terminalTicketsRejectEveryTransitionButAllowFieldsAndComments(
            TicketStatus terminalStatus
    ) throws Exception {
        UUID ticketId = createTicket();
        setStatus(ticketId, terminalStatus);

        for (TicketStatus target : TicketStatus.values()) {
            mockMvc.perform(post("/api/v1/tickets/{ticketId}/transitions", ticketId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"targetStatus": "%s"}
                                    """.formatted(target)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
            assertThat(persistedStatus(ticketId)).isEqualTo(terminalStatus);
            assertThat(persistedUpdatedAt(ticketId)).isEqualTo(BASELINE_UPDATED_AT);
        }

        mockMvc.perform(patch("/api/v1/tickets/{ticketId}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Updated terminal ticket"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated terminal ticket"))
                .andExpect(jsonPath("$.status").value(terminalStatus.name()));

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "Terminal status comment"}
                                """))
                .andExpect(status().isCreated());

        assertThat(persistedStatus(ticketId)).isEqualTo(terminalStatus);
    }

    @Test
    void trimsTargetStatus() throws Exception {
        UUID ticketId = createTicket();

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/transitions", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetStatus": "  IN_PROGRESS  "}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void rejectsUnknownMissingNullAndBlankTargets() throws Exception {
        UUID ticketId = createTicket();

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/transitions", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetStatus": "UNKNOWN"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        for (String body : new String[]{
                "{}",
                "{\"targetStatus\": null}",
                "{\"targetStatus\": \"   \"}"
        }) {
            mockMvc.perform(post("/api/v1/tickets/{ticketId}/transitions", ticketId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().is(422))
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("targetStatus"));
        }

        assertThat(persistedStatus(ticketId)).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    void returnsNotFoundForMissingTicket() throws Exception {
        mockMvc.perform(post("/api/v1/tickets/{ticketId}/transitions", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetStatus": "IN_PROGRESS"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
    }

    @Test
    void rejectsMalformedTicketIdAndUnknownProperties() throws Exception {
        mockMvc.perform(post("/api/v1/tickets/not-a-uuid/transitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetStatus": "IN_PROGRESS"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        UUID ticketId = createTicket();
        mockMvc.perform(post("/api/v1/tickets/{ticketId}/transitions", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetStatus": "IN_PROGRESS",
                                  "status": "CLOSED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        assertThat(persistedStatus(ticketId)).isEqualTo(TicketStatus.OPEN);
    }

    private static Stream<Arguments> allowedTransitions() {
        return Stream.of(
                Arguments.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS),
                Arguments.of(TicketStatus.OPEN, TicketStatus.CANCELLED),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
                Arguments.of(TicketStatus.RESOLVED, TicketStatus.CLOSED)
        );
    }

    private static Stream<Arguments> representativeInvalidTransitions() {
        return Stream.of(
                Arguments.of(TicketStatus.RESOLVED, TicketStatus.OPEN),
                Arguments.of(TicketStatus.OPEN, TicketStatus.RESOLVED),
                Arguments.of(TicketStatus.OPEN, TicketStatus.CLOSED),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.OPEN),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED),
                Arguments.of(TicketStatus.RESOLVED, TicketStatus.IN_PROGRESS),
                Arguments.of(TicketStatus.RESOLVED, TicketStatus.CANCELLED),
                Arguments.of(TicketStatus.OPEN, TicketStatus.OPEN),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.IN_PROGRESS),
                Arguments.of(TicketStatus.RESOLVED, TicketStatus.RESOLVED)
        );
    }

    private UUID createTicket() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Transition ticket",
                                  "description": "Transition test",
                                  "priority": "MEDIUM"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String id = jsonMapper.readTree(
                result.getResponse().getContentAsByteArray()
        ).get("id").stringValue();
        return UUID.fromString(id);
    }

    private void setStatus(UUID ticketId, TicketStatus status) {
        jdbcTemplate.update(
                "UPDATE tickets SET status = ?, updated_at = ? WHERE id = ?",
                status.name(),
                BASELINE_UPDATED_AT,
                ticketId
        );
    }

    private TicketStatus persistedStatus(UUID ticketId) {
        return TicketStatus.valueOf(jdbcTemplate.queryForObject(
                "SELECT status FROM tickets WHERE id = ?",
                String.class,
                ticketId
        ));
    }

    private OffsetDateTime persistedUpdatedAt(UUID ticketId) {
        return jdbcTemplate.queryForObject(
                "SELECT updated_at FROM tickets WHERE id = ?",
                OffsetDateTime.class,
                ticketId
        );
    }
}
