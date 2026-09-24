# Ticket State Machine

**Status:** Approved for implementation  
**Sources:** `doc/Assessments.pdf`, `spec/requirements.md` FR-008  
**Last updated:** 2026-09-23

## 1. Purpose

Define the complete backend-enforced lifecycle for a support ticket. This document is authoritative for status changes.

## 2. States

- `OPEN` — initial state assigned by the server.
- `IN_PROGRESS` — work has started.
- `RESOLVED` — work is complete but not yet closed.
- `CLOSED` — terminal successful state.
- `CANCELLED` — terminal abandoned state.

Clients cannot choose the initial status during ticket creation.

## 3. Allowed transitions

Only these directed transitions are valid:

- `OPEN` → `IN_PROGRESS`
- `OPEN` → `CANCELLED`
- `IN_PROGRESS` → `RESOLVED`
- `IN_PROGRESS` → `CANCELLED`
- `RESOLVED` → `CLOSED`

## 4. Complete transition rules

- From `OPEN`, allow only `IN_PROGRESS` and `CANCELLED`.
- From `IN_PROGRESS`, allow only `RESOLVED` and `CANCELLED`.
- From `RESOLVED`, allow only `CLOSED`.
- From `CLOSED`, allow no target state.
- From `CANCELLED`, allow no target state.
- Reject every self-transition.
- Reject null, unknown, or malformed target status as input validation errors.

Examples explicitly rejected by the assessment:

- `CLOSED` → `OPEN`
- `RESOLVED` → `OPEN`
- `CANCELLED` → `OPEN`

Additional representative rejections:

- `OPEN` → `RESOLVED`
- `OPEN` → `CLOSED`
- `IN_PROGRESS` → `OPEN`
- `IN_PROGRESS` → `CLOSED`
- `RESOLVED` → `IN_PROGRESS`
- `RESOLVED` → `CANCELLED`
- Any status → the same status

## 5. Backend enforcement

1. Load the ticket by identifier.
2. Return not-found if it does not exist.
3. Validate the requested target status.
4. Ask one domain transition policy whether the current-target pair is allowed.
5. If invalid, return a conflict and do not modify or save the ticket.
6. If valid, update status and `updatedAt` in one transaction.
7. Return the updated ticket representation.

No controller, repository, migration, generic field-update operation, or client may bypass this policy. The field-update operation does not accept `status`.

## 6. Error semantics

- Malformed or unknown target status: HTTP `400 Bad Request`.
- Missing, null, blank, or whitespace-only target status: HTTP `422 Unprocessable Entity`.
- Existing ticket with a disallowed current-target pair: HTTP `409 Conflict`.
- Missing ticket: HTTP `404 Not Found`.
- Error responses follow the problem format in `api-contract.md`.
- Invalid transitions leave status and all other persisted values unchanged.

## 7. Terminal-state behavior

`CLOSED` and `CANCELLED` are terminal only for status. The approved requirements still allow:

- Updates to title, description, priority, and assignee.
- New comments.

These operations cannot alter status.

## 8. Invariants

- Every persisted ticket has exactly one recognized status.
- Every new ticket starts as `OPEN`.
- Status changes occur only along an allowed edge.
- A rejected transition performs no persistence update.
- A successful transition survives application restart.
- Concurrent transition behavior follows last successful write wins; optimistic concurrency is not part of the initial contract.

## 9. Required tests

- One integration test for each of the five allowed transitions.
- Rejection tests for all three invalid examples in the assessment.
- Rejection tests for the additional representative cases in section 4.
- A self-transition rejection test for every state.
- For both `CLOSED` and `CANCELLED`, tests that all status changes are rejected while field updates and comment creation remain allowed.
- Tests that rejected transitions leave persisted status unchanged.
- Tests for malformed status and missing ticket.
