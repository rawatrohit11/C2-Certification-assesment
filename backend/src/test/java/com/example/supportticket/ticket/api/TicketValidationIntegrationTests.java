package com.example.supportticket.ticket.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TicketValidationIntegrationTests {

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

    @ParameterizedTest
    @ValueSource(strings = {"LOW", "MEDIUM", "HIGH", "URGENT"})
    void acceptsExactTextBoundariesAndEveryPriority(String priority) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "%s",
                                  "priority": "%s",
                                  "assignee": "%s"
                                }
                                """.formatted(
                                "T".repeat(200),
                                "D".repeat(5000),
                                priority,
                                "A".repeat(100)
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority").value(priority))
                .andReturn();

        JsonNode response = responseJson(result);
        assertThat(response.get("title").stringValue()).hasSize(200);
        assertThat(response.get("description").stringValue()).hasSize(5000);
        assertThat(response.get("assignee").stringValue()).hasSize(100);
    }

    @ParameterizedTest
    @MethodSource("invalidCreateBoundaries")
    void rejectsCreateValuesAboveBoundaries(String body, String field) throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(422))
                .andExpect(header().string(
                        "Content-Type",
                        MediaType.APPLICATION_PROBLEM_JSON_VALUE
                ))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.instance").value("/api/v1/tickets"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value(field));
    }

    @ParameterizedTest
    @MethodSource("missingAndNullRequiredFields")
    void rejectsMissingNullAndEmptyRequiredFields(String body, String field) throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value(field));
    }

    @Test
    void rejectsUnknownPriority() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Title",
                                  "description": "Description",
                                  "priority": "CRITICAL"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @ParameterizedTest
    @MethodSource("invalidUpdateBoundaries")
    void rejectsUpdateValuesAboveBoundaries(String field, String value) throws Exception {
        UUID ticketId = createTicket();

        mockMvc.perform(patch("/api/v1/tickets/{ticketId}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"%s": "%s"}
                                """.formatted(field, value)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value(field));
    }

    @Test
    void validatesCommentMaximumBoundary() throws Exception {
        UUID ticketId = createTicket();

        MvcResult result = mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "%s"}
                                """.formatted("C".repeat(2000))))
                .andExpect(status().isCreated())
                .andReturn();
        assertThat(responseJson(result).get("body").stringValue()).hasSize(2000);

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "%s"}
                                """.formatted("C".repeat(2001))))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("body"));
    }

    @ParameterizedTest
    @MethodSource("invalidPagination")
    void rejectsEveryInvalidPaginationBoundary(String name, String value) throws Exception {
        mockMvc.perform(get("/api/v1/tickets").param(name, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void ordersPagesByCreatedAtThenIdDescending() throws Exception {
        OffsetDateTime older = OffsetDateTime.parse("2026-09-22T10:00:00Z");
        OffsetDateTime newer = OffsetDateTime.parse("2026-09-23T10:00:00Z");
        UUID olderId = UUID.fromString("10000000-0000-0000-0000-000000000000");
        UUID lowerNewId = UUID.fromString("20000000-0000-0000-0000-000000000000");
        UUID higherNewId = UUID.fromString("30000000-0000-0000-0000-000000000000");

        insertTicket(olderId, "Older", older);
        insertTicket(lowerNewId, "Newer lower ID", newer);
        insertTicket(higherNewId, "Newer higher ID", newer);

        mockMvc.perform(get("/api/v1/tickets").param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(higherNewId.toString()))
                .andExpect(jsonPath("$.content[1].id").value(lowerNewId.toString()))
                .andExpect(jsonPath("$.content[2].id").value(olderId.toString()));
    }

    private static Stream<Arguments> invalidCreateBoundaries() {
        return Stream.of(
                Arguments.of(createBody("T".repeat(201), "Description", "A"), "title"),
                Arguments.of(createBody("Title", "D".repeat(5001), "A"), "description"),
                Arguments.of(createBody("Title", "Description", "A".repeat(101)), "assignee")
        );
    }

    private static Stream<Arguments> missingAndNullRequiredFields() {
        return Stream.of(
                Arguments.of(
                        "{\"description\":\"Description\",\"priority\":\"LOW\"}",
                        "title"
                ),
                Arguments.of(
                        "{\"title\":null,\"description\":\"Description\",\"priority\":\"LOW\"}",
                        "title"
                ),
                Arguments.of(
                        "{\"title\":\"\",\"description\":\"Description\",\"priority\":\"LOW\"}",
                        "title"
                ),
                Arguments.of(
                        "{\"title\":\"Title\",\"priority\":\"LOW\"}",
                        "description"
                ),
                Arguments.of(
                        "{\"title\":\"Title\",\"description\":null,\"priority\":\"LOW\"}",
                        "description"
                ),
                Arguments.of(
                        "{\"title\":\"Title\",\"description\":\"\",\"priority\":\"LOW\"}",
                        "description"
                ),
                Arguments.of(
                        "{\"title\":\"Title\",\"description\":\"Description\"}",
                        "priority"
                ),
                Arguments.of(
                        "{\"title\":\"Title\",\"description\":\"Description\",\"priority\":null}",
                        "priority"
                )
        );
    }

    private static Stream<Arguments> invalidUpdateBoundaries() {
        return Stream.of(
                Arguments.of("title", "T".repeat(201)),
                Arguments.of("description", "D".repeat(5001)),
                Arguments.of("assignee", "A".repeat(101))
        );
    }

    private static Stream<Arguments> invalidPagination() {
        return Stream.of(
                Arguments.of("page", "-1"),
                Arguments.of("size", "0"),
                Arguments.of("size", "101")
        );
    }

    private static String createBody(String title, String description, String assignee) {
        return """
                {
                  "title": "%s",
                  "description": "%s",
                  "priority": "LOW",
                  "assignee": "%s"
                }
                """.formatted(title, description, assignee);
    }

    private UUID createTicket() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Title",
                                  "description": "Description",
                                  "priority": "LOW"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(responseJson(result).get("id").stringValue());
    }

    private JsonNode responseJson(MvcResult result) throws Exception {
        return jsonMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private void insertTicket(UUID id, String title, OffsetDateTime timestamp) {
        jdbcTemplate.update(
                """
                INSERT INTO tickets (
                    id, title, description, priority, status, assignee, created_at, updated_at
                ) VALUES (?, ?, 'Description', 'LOW', 'OPEN', NULL, ?, ?)
                """,
                id,
                title,
                timestamp,
                timestamp
        );
    }
}
