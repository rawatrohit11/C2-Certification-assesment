# Support Ticket Management System — Requirements

**Status:** Approved for specification  
**Authoritative source:** `doc/Assessments.pdf`  
**Last updated:** 2026-09-23

## 1. Purpose

Define the required behavior and delivery constraints for a Support Ticket Management System. This document records product requirements only; architecture and implementation decisions belong in later specification artifacts.

## 2. Goals

- Enable a support team member to create and manage support tickets through a web UI.
- Make current ticket state, comments, and persisted field values visible.
- Enforce the prescribed ticket lifecycle on the backend.
- Reject invalid input safely and show meaningful errors to users.
- Demonstrate a traceable, spec-driven workflow using AI as a reviewed engineering assistant.

## 3. Actors

### Support team member

Uses the UI to create, find, inspect, update, assign, comment on, and transition tickets.

“Support team member” is the chosen name for the unauthenticated actor implied by the assessment. Authentication, authorization, and distinct user roles are not required and remain out of scope unless added later.

## 4. Functional requirements

### FR-001 — Create a ticket

- The system shall allow a support team member to create a ticket from the UI.
- The backend shall validate all submitted ticket data.
- A successfully created ticket shall be persisted and assigned a stable identifier.
- The server shall assign `OPEN`; create requests shall not accept a client-supplied status.

### FR-002 — List tickets

- The system shall display persisted tickets in the UI.
- Each list item shall provide enough information to identify and open the ticket.
- Tickets shall be ordered newest-first and returned in pages.
- Empty results shall be represented as a valid, meaningful UI state.

### FR-003 — View ticket details

- The system shall allow a support team member to view one ticket by its identifier.
- The detail view shall include the ticket's editable fields, current status, and comments.
- A missing ticket shall produce a meaningful API error and UI message.

### FR-004 — Update ticket fields

- The system shall allow updates to a ticket's title, description, priority, and assignee.
- The backend shall validate updated values.
- A successful update shall persist across application restarts.
- Field updates shall not bypass status-transition rules.
- Title, description, priority, and assignee may be updated in every status, including `CLOSED` and `CANCELLED`.

### FR-005 — Add comments

- The system shall allow a comment to be added to an existing ticket.
- The backend shall validate comment input.
- Added comments shall be persisted with a server-generated timestamp and displayed oldest-first with the ticket details.
- Comments may be added in every ticket status, including `CLOSED` and `CANCELLED`.

### FR-006 — Search tickets

- The system shall allow tickets to be searched by keyword.
- Search shall perform a case-insensitive substring match over title and description.
- A blank or whitespace-only keyword shall behave as no keyword filter.
- The UI shall distinguish no matches from a request failure.

### FR-007 — Filter tickets by status

- The system shall allow tickets to be filtered by one valid ticket status.
- The backend shall reject an unknown status value.
- Search and status filtering shall work together when both are supplied.

### FR-008 — Transition ticket status

- The backend shall enforce the ticket state machine defined in section 5.
- Valid transitions shall persist and be visible in subsequent reads.
- Invalid transitions shall be rejected without changing persisted ticket state.
- UI controls shall not present an invalid transition as a successful action.

### FR-009 — Persist data

- Tickets, field updates, status changes, assignee changes, and comments shall survive application restart.
- PostgreSQL shall be the default durable runtime database.
- H2 may be used for automated tests, but in-memory H2 shall not be used to demonstrate restart persistence.

### FR-010 — Validate input on the backend

- Backend validation shall be authoritative even when the UI also validates input.
- Validation errors shall identify invalid fields without exposing stack traces or internal implementation details.
- Title shall be required after trimming and contain 1–200 characters.
- Description shall be required after trimming and contain 1–5000 characters.
- Priority shall be required and one of `LOW`, `MEDIUM`, `HIGH`, or `URGENT`.
- Assignee shall be optional; when supplied it shall contain 1–100 characters after trimming.
- Comment body shall be required after trimming and contain 1–2000 characters.

### FR-011 — Display meaningful UI errors

- The UI shall show safe, actionable messages for validation failures, missing tickets, invalid state transitions, connectivity failures, and unexpected server errors.
- Failed operations shall not be displayed as successful.
- Technical details not useful to the user shall remain out of the UI.

## 5. Ticket state machine

The allowed transitions are:

- `OPEN` → `IN_PROGRESS`
- `OPEN` → `CANCELLED`
- `IN_PROGRESS` → `RESOLVED`
- `IN_PROGRESS` → `CANCELLED`
- `RESOLVED` → `CLOSED`

`CLOSED` and `CANCELLED` are terminal states. All transitions not listed above are invalid, including self-transitions. This is the approved interpretation of the assessment state machine.

The backend is the authority for transition validation. Hiding invalid actions in the UI is supplementary and must not replace backend enforcement.

## 6. Technical and delivery constraints

- Backend: Java 21 and Spring Boot.
- Persistence: PostgreSQL and/or H2, with data persistence demonstrated.
- Integration style: REST API.
- Frontend: React with Vite.
- Required development tools: Cursor and GitHub Copilot.
- Delivery workflow: requirements → specification → plan/tasks → implementation → testing → review → fix.
- Specifications shall exist before implementation.
- Submitted prompts and meaningful AI corrections shall be retained as assessment evidence.
- Token usage shall be managed with scoped rules, skills, narrow repository searches, and verified helper tooling; unverified plugins shall not be added solely for appearance.
- No secrets shall be committed.

## 7. Core acceptance criteria

- **AC-001:** A ticket can be created from the UI.
- **AC-002:** Persisted tickets can be listed.
- **AC-003:** Ticket details can be viewed.
- **AC-004:** Title, description, and priority can be updated.
- **AC-005:** Assignee can be changed.
- **AC-006:** Comments can be added and subsequently viewed.
- **AC-007:** Keyword search returns matching tickets.
- **AC-008:** Status filtering returns tickets with the selected status.
- **AC-009:** Every allowed status transition succeeds.
- **AC-010:** Invalid status transitions are rejected by the backend without changing state.
- **AC-011:** Data survives an application restart.
- **AC-012:** Invalid backend input is rejected with a meaningful response.
- **AC-013:** The UI displays meaningful errors for failed operations.
- **AC-014:** State-machine integration tests pass.
- **AC-015:** The repository contains no committed secrets.

## 8. Traceability

- FR-001 maps to AC-001, AC-011, AC-012, and AC-013.
- FR-002 maps to AC-002 and AC-013.
- FR-003 maps to AC-003 and AC-013.
- FR-004 maps to AC-004, AC-005, AC-011, and AC-012.
- FR-005 maps to AC-006, AC-011, and AC-012.
- FR-006 maps to AC-007 and AC-013.
- FR-007 maps to AC-008 and AC-012.
- FR-008 maps to AC-009, AC-010, AC-011, AC-013, and AC-014.
- FR-009 maps to AC-011.
- FR-010 maps to AC-012.
- FR-011 maps to AC-013.
- The no-secrets constraint in section 6 maps to AC-015.

## 9. Out of scope

Unless a later approved requirement adds them:

- User registration, login, roles, and permissions
- Ticket deletion
- Attachments
- Email or real-time notifications
- Service-level agreements and escalation
- Reporting and analytics
- Comment editing or deletion
- Integrations with external ticketing systems

## 10. Approved product decisions

- React with Vite is the frontend.
- PostgreSQL is the default runtime database; H2 is for automated tests.
- Priorities are `LOW`, `MEDIUM`, `HIGH`, and `URGENT`; no implicit default is assigned.
- Assignee is optional trimmed text, not a user entity.
- Comments are append-only, have server-generated timestamps, and are displayed oldest-first. No author is stored because authentication is out of scope.
- Search is a case-insensitive substring match over title and description.
- Ticket lists are newest-first and paginated.
- Partial field updates use a dedicated update operation; status changes use a separate transition operation.
- API timestamps use UTC ISO 8601 values.
- The initial version has no optimistic-concurrency contract; the last successful write wins.
- Field updates and comments remain allowed in terminal statuses; status does not.
- Self-transitions are invalid.

## 11. Specification completion gate

No open product decision blocks the remaining specification artifacts. Any new ambiguity found while writing them must be recorded here and resolved before implementation.
