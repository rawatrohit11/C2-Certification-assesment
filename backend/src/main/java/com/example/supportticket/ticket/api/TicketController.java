package com.example.supportticket.ticket.api;

import com.example.supportticket.shared.error.MalformedRequestException;
import com.example.supportticket.ticket.application.TicketService;
import com.example.supportticket.ticket.domain.TicketStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
@Validated
public class TicketController {

    private static final Set<String> LIST_QUERY_PARAMETERS =
            Set.of("keyword", "status", "page", "size");

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketDetailResponse> create(
            @Valid @RequestBody CreateTicketRequest request
    ) {
        TicketDetailResponse created = ticketService.create(request);
        return ResponseEntity
                .created(URI.create("/api/v1/tickets/" + created.id()))
                .body(created);
    }

    @GetMapping
    public TicketPageResponse list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam MultiValueMap<String, String> queryParameters
    ) {
        Set<String> unknownParameters = new HashSet<>(queryParameters.keySet());
        unknownParameters.removeAll(LIST_QUERY_PARAMETERS);
        if (!unknownParameters.isEmpty()) {
            throw new MalformedRequestException(
                    "Unknown query parameter: " + unknownParameters.iterator().next()
            );
        }
        return ticketService.list(keyword, status, page, size);
    }

    @GetMapping("/{ticketId}")
    public TicketDetailResponse get(@PathVariable UUID ticketId) {
        return ticketService.get(ticketId);
    }

    @PatchMapping("/{ticketId}")
    public TicketDetailResponse update(
            @PathVariable UUID ticketId,
            @RequestBody UpdateTicketRequest request
    ) {
        return ticketService.update(ticketId, request);
    }

    @PostMapping("/{ticketId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable UUID ticketId,
            @Valid @RequestBody AddCommentRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ticketService.addComment(ticketId, request));
    }

    @PostMapping("/{ticketId}/transitions")
    public TicketDetailResponse transition(
            @PathVariable UUID ticketId,
            @Valid @RequestBody TransitionTicketRequest request
    ) {
        return ticketService.transition(ticketId, request);
    }
}
