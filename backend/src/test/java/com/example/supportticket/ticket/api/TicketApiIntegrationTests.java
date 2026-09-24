package com.example.supportticket.ticket.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TicketApiIntegrationTests {

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

    @Test
    void createsOpenTicketWithTrimmedInput() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "  Cannot access billing  ",
                                  "description": "  Billing page returns an error.  ",
                                  "priority": "HIGH",
                                  "assignee": "  Avery Singh  "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Cannot access billing"))
                .andExpect(jsonPath("$.description").value("Billing page returns an error."))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignee").value("Avery Singh"))
                .andExpect(jsonPath("$.comments").isEmpty())
                .andReturn();

        String id = responseJson(result).get("id").stringValue();
        assertThat(result.getResponse().getHeader("Location"))
                .isEqualTo("/api/v1/tickets/" + id);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tickets", Integer.class))
                .isOne();
    }

    @Test
    void rejectsInvalidCreateFieldsWithProblemDetails() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "   ",
                                  "description": "Description",
                                  "priority": "LOW"
                                }
                                """))
                .andExpect(status().is(422))
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));
    }

    @Test
    void rejectsProtectedCreateFields() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Title",
                                  "description": "Description",
                                  "priority": "LOW",
                                  "status": "CLOSED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void listsTicketsAndReturnsDetails() throws Exception {
        UUID ticketId = createTicket("First ticket", "Description", "MEDIUM", null);
        createTicket("Second ticket", "Description", "HIGH", "Taylor");

        mockMvc.perform(get("/api/v1/tickets")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/v1/tickets/{ticketId}", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId.toString()))
                .andExpect(jsonPath("$.title").value("First ticket"))
                .andExpect(jsonPath("$.comments").isEmpty());
    }

    @Test
    void returnsEmptyTicketPage() throws Exception {
        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void searchesTitleAndDescriptionCaseInsensitively() throws Exception {
        UUID billingId = createTicket(
                "Billing failure",
                "Customer cannot open the account page",
                "HIGH",
                null
        );
        UUID printerId = createTicket(
                "Office issue",
                "The PRINTER is unavailable",
                "LOW",
                null
        );

        mockMvc.perform(get("/api/v1/tickets").param("keyword", "  BiLlInG  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(billingId.toString()));

        mockMvc.perform(get("/api/v1/tickets").param("keyword", "printer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(printerId.toString()));
    }

    @Test
    void treatsSearchWildcardCharactersAsLiteralText() throws Exception {
        UUID percentId = createTicket("100% outage", "Description", "HIGH", null);
        createTicket("Ordinary outage", "Description", "LOW", null);

        mockMvc.perform(get("/api/v1/tickets").param("keyword", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(percentId.toString()));
    }

    @Test
    void combinesKeywordAndStatusFilters() throws Exception {
        UUID openId = createTicket("Billing open", "Description", "LOW", null);
        UUID resolvedId = createTicket("Billing resolved", "Description", "HIGH", null);
        jdbcTemplate.update(
                "UPDATE tickets SET status = 'RESOLVED' WHERE id = ?",
                resolvedId
        );

        mockMvc.perform(get("/api/v1/tickets")
                        .param("keyword", "billing")
                        .param("status", "RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(resolvedId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("RESOLVED"));

        mockMvc.perform(get("/api/v1/tickets")
                        .param("keyword", "billing")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(openId.toString()));
    }

    @Test
    void filtersByStatusAndReturnsEmptyResults() throws Exception {
        UUID resolvedId = createTicket("Resolved", "Description", "LOW", null);
        createTicket("Open", "Description", "HIGH", null);
        jdbcTemplate.update(
                "UPDATE tickets SET status = 'RESOLVED' WHERE id = ?",
                resolvedId
        );

        mockMvc.perform(get("/api/v1/tickets").param("status", "RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(resolvedId.toString()));

        mockMvc.perform(get("/api/v1/tickets").param("keyword", "no matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void treatsBlankKeywordAsNoFilter() throws Exception {
        createTicket("First ticket", "Description", "LOW", null);
        createTicket("Second ticket", "Description", "HIGH", null);

        mockMvc.perform(get("/api/v1/tickets").param("keyword", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void rejectsUnknownStatusAndQueryParameter() throws Exception {
        mockMvc.perform(get("/api/v1/tickets").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        mockMvc.perform(get("/api/v1/tickets").param("szie", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void returnsProblemWhenTicketDoesNotExist() throws Exception {
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/tickets/{ticketId}", missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void rejectsMalformedTicketId() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void updatesEditableFieldsWithoutChangingStatus() throws Exception {
        UUID ticketId = createTicket("Original", "Original description", "LOW", "Taylor");

        mockMvc.perform(patch("/api/v1/tickets/{ticketId}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "  Updated  ",
                                  "description": "Updated description",
                                  "priority": "URGENT",
                                  "assignee": "  Morgan  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.priority").value("URGENT"))
                .andExpect(jsonPath("$.assignee").value("Morgan"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void rejectsNullRequiredUpdateFields() throws Exception {
        UUID ticketId = createTicket("Title", "Description", "LOW", null);

        mockMvc.perform(patch("/api/v1/tickets/{ticketId}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": null,
                                  "priority": null
                                }
                                """))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }

    @Test
    void returnsProblemWhenUpdatingMissingTicket() throws Exception {
        mockMvc.perform(patch("/api/v1/tickets/{ticketId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Updated"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
    }

    @Test
    void blankAssigneeClearsAssignment() throws Exception {
        UUID ticketId = createTicket("Title", "Description", "LOW", "Taylor");

        mockMvc.perform(patch("/api/v1/tickets/{ticketId}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assignee": "   "}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee").value(nullValue()));
    }

    @Test
    void addsTrimmedCommentsInOldestFirstOrderWithoutUpdatingTicket() throws Exception {
        UUID ticketId = createTicket("Title", "Description", "LOW", null);
        String updatedAt = responseJson(mockMvc.perform(
                        get("/api/v1/tickets/{ticketId}", ticketId)
                ).andReturn()).get("updatedAt").stringValue();

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "  First comment  "}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("First comment"));

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "Second comment"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/tickets/{ticketId}", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedAt").value(updatedAt))
                .andExpect(jsonPath("$.comments.length()").value(2))
                .andExpect(jsonPath("$.comments[0].body").value("First comment"))
                .andExpect(jsonPath("$.comments[1].body").value("Second comment"));
    }

    @Test
    void rejectsInvalidCommentAndMissingTicket() throws Exception {
        UUID ticketId = createTicket("Title", "Description", "LOW", null);

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "   "}
                                """))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("body"));

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "Comment"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
    }

    @Test
    void rejectsEmptyOrProtectedUpdates() throws Exception {
        UUID ticketId = createTicket("Title", "Description", "LOW", null);

        mockMvc.perform(patch("/api/v1/tickets/{ticketId}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(patch("/api/v1/tickets/{ticketId}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "CLOSED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        mockMvc.perform(get("/api/v1/tickets/{ticketId}", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void rejectsInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/v1/tickets").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    private UUID createTicket(
            String title,
            String description,
            String priority,
            String assignee
    ) throws Exception {
        String assigneeJson = assignee == null ? "null" : "\"" + assignee + "\"";
        MvcResult result = mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "%s",
                                  "priority": "%s",
                                  "assignee": %s
                                }
                                """.formatted(title, description, priority, assigneeJson)))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(responseJson(result).get("id").stringValue());
    }

    private JsonNode responseJson(MvcResult result) throws Exception {
        return jsonMapper.readTree(result.getResponse().getContentAsByteArray());
    }
}
