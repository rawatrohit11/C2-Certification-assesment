package com.example.supportticket.ticket.application;

import com.example.supportticket.shared.error.FieldErrorDetail;
import com.example.supportticket.shared.error.MalformedRequestException;
import com.example.supportticket.ticket.api.AddCommentRequest;
import com.example.supportticket.ticket.api.CommentResponse;
import com.example.supportticket.ticket.api.CreateTicketRequest;
import com.example.supportticket.ticket.api.TicketDetailResponse;
import com.example.supportticket.ticket.api.TicketPageResponse;
import com.example.supportticket.ticket.api.TicketSummaryResponse;
import com.example.supportticket.ticket.api.TransitionTicketRequest;
import com.example.supportticket.ticket.api.UpdateTicketRequest;
import com.example.supportticket.ticket.domain.TicketStatus;
import com.example.supportticket.ticket.domain.TicketTransitionPolicy;
import com.example.supportticket.ticket.persistence.CommentEntity;
import com.example.supportticket.ticket.persistence.CommentRepository;
import com.example.supportticket.ticket.persistence.TicketEntity;
import com.example.supportticket.ticket.persistence.TicketRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TicketService {

    private static final Sort NEWEST_FIRST = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id")
    );

    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final TicketTransitionPolicy transitionPolicy;
    private final Clock clock;

    public TicketService(
            TicketRepository ticketRepository,
            CommentRepository commentRepository,
            TicketTransitionPolicy transitionPolicy,
            Clock clock
    ) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.transitionPolicy = transitionPolicy;
        this.clock = clock;
    }

    @Transactional
    public TicketDetailResponse create(CreateTicketRequest request) {
        Instant now = clock.instant();
        TicketEntity ticket = TicketEntity.create(
                UUID.randomUUID(),
                request.title(),
                request.description(),
                request.priority(),
                request.assignee(),
                now
        );

        return toDetail(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public TicketPageResponse list(
            String keyword,
            TicketStatus status,
            int page,
            int size
    ) {
        String normalizedKeyword = normalizeKeyword(keyword);
        PageRequest pageRequest = PageRequest.of(page, size, NEWEST_FIRST);
        Page<TicketEntity> tickets;

        if (normalizedKeyword != null && status != null) {
            tickets = ticketRepository
                    .findByStatusAndTitleContainingIgnoreCaseOrStatusAndDescriptionContainingIgnoreCase(
                            status,
                            normalizedKeyword,
                            status,
                            normalizedKeyword,
                            pageRequest
                    );
        } else if (normalizedKeyword != null) {
            tickets = ticketRepository
                    .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                            normalizedKeyword,
                            normalizedKeyword,
                            pageRequest
                    );
        } else if (status != null) {
            tickets = ticketRepository.findByStatus(status, pageRequest);
        } else {
            tickets = ticketRepository.findAll(pageRequest);
        }

        return new TicketPageResponse(
                tickets.getContent().stream().map(this::toSummary).toList(),
                tickets.getNumber(),
                tickets.getSize(),
                tickets.getTotalElements(),
                tickets.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public TicketDetailResponse get(UUID ticketId) {
        return toDetail(findTicket(ticketId));
    }

    @Transactional
    public TicketDetailResponse update(UUID ticketId, UpdateTicketRequest request) {
        validateUpdate(request);
        TicketEntity ticket = findTicket(ticketId);

        ticket.updateFields(
                request.isTitlePresent() ? request.getTitle() : ticket.getTitle(),
                request.isDescriptionPresent() ? request.getDescription() : ticket.getDescription(),
                request.isPriorityPresent() ? request.getPriority() : ticket.getPriority(),
                request.isAssigneePresent() ? request.getAssignee() : ticket.getAssignee(),
                clock.instant()
        );

        return toDetail(ticket);
    }

    @Transactional
    public CommentResponse addComment(UUID ticketId, AddCommentRequest request) {
        findTicket(ticketId);
        CommentEntity comment = CommentEntity.create(
                UUID.randomUUID(),
                ticketId,
                request.body(),
                clock.instant()
        );
        return toComment(commentRepository.save(comment));
    }

    @Transactional
    public TicketDetailResponse transition(
            UUID ticketId,
            TransitionTicketRequest request
    ) {
        TicketEntity ticket = findTicket(ticketId);
        TicketStatus targetStatus = parseTargetStatus(request.targetStatus());
        if (!transitionPolicy.canTransition(ticket.getStatus(), targetStatus)) {
            throw new InvalidStatusTransitionException(ticket.getStatus(), targetStatus);
        }

        ticket.transitionTo(targetStatus, clock.instant());
        return toDetail(ticket);
    }

    private TicketEntity findTicket(UUID ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
    }

    private void validateUpdate(UpdateTicketRequest request) {
        List<FieldErrorDetail> errors = new ArrayList<>();

        if (!request.hasAnyField()) {
            errors.add(new FieldErrorDetail("request", "must contain at least one editable field"));
        }
        if (request.isTitlePresent()) {
            validateRequiredLength("title", request.getTitle(), 200, errors);
        }
        if (request.isDescriptionPresent()) {
            validateRequiredLength("description", request.getDescription(), 5000, errors);
        }
        if (request.isPriorityPresent() && request.getPriority() == null) {
            errors.add(new FieldErrorDetail("priority", "must not be null"));
        }
        if (request.isAssigneePresent()
                && request.getAssignee() != null
                && request.getAssignee().length() > 100) {
            errors.add(new FieldErrorDetail("assignee", "must contain at most 100 characters"));
        }

        if (!errors.isEmpty()) {
            throw new TicketInputException("Ticket update validation failed.", errors);
        }
    }

    private void validateRequiredLength(
            String field,
            String value,
            int maximum,
            List<FieldErrorDetail> errors
    ) {
        if (value == null || value.isEmpty()) {
            errors.add(new FieldErrorDetail(field, "must not be blank"));
        } else if (value.length() > maximum) {
            errors.add(new FieldErrorDetail(
                    field,
                    "must contain between 1 and " + maximum + " characters"
            ));
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private TicketStatus parseTargetStatus(String targetStatus) {
        try {
            return TicketStatus.valueOf(targetStatus);
        } catch (IllegalArgumentException exception) {
            throw new MalformedRequestException(
                    "Unknown target status: " + targetStatus
            );
        }
    }

    private TicketSummaryResponse toSummary(TicketEntity ticket) {
        return new TicketSummaryResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getAssignee(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    private TicketDetailResponse toDetail(TicketEntity ticket) {
        return new TicketDetailResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getAssignee(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                commentRepository.findByTicketIdOrderByCreatedAtAscIdAsc(ticket.getId())
                        .stream()
                        .map(this::toComment)
                        .toList()
        );
    }

    private CommentResponse toComment(CommentEntity comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getBody(),
                comment.getCreatedAt()
        );
    }
}
