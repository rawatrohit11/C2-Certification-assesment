CREATE TABLE tickets (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    priority VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    assignee VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_tickets_title_length
        CHECK (CHAR_LENGTH(TRIM(title)) BETWEEN 1 AND 200),
    CONSTRAINT ck_tickets_description_length
        CHECK (CHAR_LENGTH(TRIM(description)) BETWEEN 1 AND 5000),
    CONSTRAINT ck_tickets_priority
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT ck_tickets_status
        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'CANCELLED')),
    CONSTRAINT ck_tickets_assignee_length
        CHECK (assignee IS NULL OR CHAR_LENGTH(TRIM(assignee)) BETWEEN 1 AND 100)
);

CREATE TABLE ticket_comments (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL,
    body VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ticket_comments_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets(id),
    CONSTRAINT ck_ticket_comments_body_length
        CHECK (CHAR_LENGTH(TRIM(body)) BETWEEN 1 AND 2000)
);

CREATE INDEX idx_tickets_created
    ON tickets (created_at DESC, id DESC);

CREATE INDEX idx_tickets_status_created
    ON tickets (status, created_at DESC, id DESC);

CREATE INDEX idx_ticket_comments_ticket_created
    ON ticket_comments (ticket_id, created_at, id);
