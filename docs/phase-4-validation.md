# Phase 4 Validation

**Date:** 2026-09-23  
**Scope:** Ticket state machine (T-019–T-020)

## Implemented behavior

- Added one domain transition policy containing exactly the five approved status edges.
- Added `POST /api/v1/tickets/{ticketId}/transitions` as the only public status mutation.
- Trimmed transition input and separated missing, null, or blank targets (`422`) from unknown targets (`400`).
- Rejected recognized but disallowed transitions with `409 INVALID_STATUS_TRANSITION`.
- Updated `status` and `updatedAt` together for successful transitions.
- Preserved status and timestamp when transitions are rejected.
- Kept field updates and comments available for `CLOSED` and `CANCELLED` tickets.

## Verification

Command:

```bash
cd backend
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw test
```

Result:

- Build succeeded on Java 21 and Spring Boot 4.1.1.
- 46 tests passed.
- The domain unit test evaluates all 25 current-target pairs and permits only the approved five.
- Integration tests cover every allowed transition, all specified representative rejections, self-transitions, every target from both terminal states, malformed input, missing tickets, persisted-state integrity, terminal-state field updates, and terminal-state comments.
- Successful transition tests verify persisted status and a changed `updatedAt`.
- Rejected transition tests verify both status and `updatedAt` remain unchanged.
- IDE diagnostics reported no errors in backend source or tests.

## Review notes

- Controllers and repositories do not contain transition rules.
- Generic ticket updates still cannot deserialize `status`.
- Invalid transitions throw before the entity mutation method is called and exit the transaction without a write.
- No dependency or schema change was required.

## Deferred checks

- PostgreSQL restart persistence for transitioned tickets remains T-024.
- Frontend transition controls and error handling begin in Phase 5.
- Final generated-test gap analysis remains T-023.
