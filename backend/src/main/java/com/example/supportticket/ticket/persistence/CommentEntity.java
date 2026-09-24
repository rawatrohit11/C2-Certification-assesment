package com.example.supportticket.ticket.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_comments")
public class CommentEntity {

    @Id
    private UUID id;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CommentEntity() {
    }

    private CommentEntity(UUID id, UUID ticketId, String body, Instant createdAt) {
        this.id = id;
        this.ticketId = ticketId;
        this.body = body;
        this.createdAt = createdAt;
    }

    public static CommentEntity create(UUID id, UUID ticketId, String body, Instant createdAt) {
        return new CommentEntity(id, ticketId, body, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
