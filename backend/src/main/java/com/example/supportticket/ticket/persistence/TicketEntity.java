package com.example.supportticket.ticket.persistence;

import com.example.supportticket.ticket.domain.TicketPriority;
import com.example.supportticket.ticket.domain.TicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets")
public class TicketEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status;

    @Column(length = 100)
    private String assignee;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TicketEntity() {
    }

    private TicketEntity(
            UUID id,
            String title,
            String description,
            TicketPriority priority,
            TicketStatus status,
            String assignee,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.assignee = assignee;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TicketEntity create(
            UUID id,
            String title,
            String description,
            TicketPriority priority,
            String assignee,
            Instant now
    ) {
        return new TicketEntity(
                id,
                title,
                description,
                priority,
                TicketStatus.OPEN,
                assignee,
                now,
                now
        );
    }

    public void updateFields(
            String title,
            String description,
            TicketPriority priority,
            String assignee,
            Instant updatedAt
    ) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.assignee = assignee;
        this.updatedAt = updatedAt;
    }

    public void transitionTo(TicketStatus targetStatus, Instant updatedAt) {
        this.status = targetStatus;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public String getAssignee() {
        return assignee;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
