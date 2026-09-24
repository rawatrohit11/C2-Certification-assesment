# REST API Contract

**Status:** Approved for implementation  
**Base path:** `/api/v1`  
**Sources:** `spec/requirements.md`, `spec/data-model.md`, `spec/state-machine.md`  
**Last updated:** 2026-09-23

## 1. Conventions

- Request and successful response bodies use `application/json`.
- Error bodies use `application/problem+json`.
- UUIDs are lowercase canonical strings.
- Timestamps are UTC ISO 8601 strings such as `2026-09-23T06:30:00Z`.
- Enum values use the exact uppercase strings in `data-model.md`.
- Backend validation is authoritative.
- All text input is trimmed before validation and persistence.
- Empty optional assignee input is normalized to null.
- Unknown or malformed enum values and malformed UUIDs return `400 Bad Request`.
- Unknown JSON properties and known non-writable properties are rejected with `400 Bad Request`; they are never silently ignored.

## 2. Resource representations

### Ticket summary

```json
{
  "id": "c06571ea-3e71-4ec7-b210-10803a04e73a",
  "title": "Cannot access billing page",
  "priority": "HIGH",
  "status": "OPEN",
  "assignee": null,
  "createdAt": "2026-09-23T06:30:00Z",
  "updatedAt": "2026-09-23T06:30:00Z"
}
```

### Ticket detail

Ticket detail contains every summary field plus `description` and `comments`.

```json
{
  "id": "c06571ea-3e71-4ec7-b210-10803a04e73a",
  "title": "Cannot access billing page",
  "description": "The billing page returns an error after sign-in.",
  "priority": "HIGH",
  "status": "OPEN",
  "assignee": null,
  "createdAt": "2026-09-23T06:30:00Z",
  "updatedAt": "2026-09-23T06:30:00Z",
  "comments": []
}
```

### Comment

```json
{
  "id": "ed2eddb4-6531-4876-b8fe-713bac644ebb",
  "body": "Issue reproduced and under investigation.",
  "createdAt": "2026-09-23T06:45:00Z"
}
```

Comments in ticket detail are ordered by `createdAt` ascending, then `id` ascending.

## 3. Create ticket

`POST /api/v1/tickets`

Request:

```json
{
  "title": "Cannot access billing page",
  "description": "The billing page returns an error after sign-in.",
  "priority": "HIGH",
  "assignee": null
}
```

Rules:

- `title`, `description`, and `priority` are required.
- `assignee` is optional.
- `status`, IDs, timestamps, comments, and any unknown properties are rejected create fields.
- The server assigns `OPEN`.

Success:

- `201 Created`
- Body: ticket detail with an empty comments array.
- `Location: /api/v1/tickets/{id}`

Errors:

- `400` with `MALFORMED_REQUEST` for malformed JSON, unknown enums, unknown properties, or non-writable properties.
- `422` with `VALIDATION_FAILED` for missing, null, blank, or out-of-range fields.

## 4. List, search, and filter tickets

`GET /api/v1/tickets`

Optional query parameters:

- `keyword` — trimmed case-insensitive substring search over title and description. Blank means no keyword filter.
- `status` — one exact `TicketStatus`. Unknown values return `400`.
- `page` — zero-based integer, default `0`, minimum `0`.
- `size` — integer, default `20`, minimum `1`, maximum `100`.

`keyword` and `status` may be combined and use AND semantics. Ordering is always `createdAt` descending, then `id` descending.

Success: `200 OK`

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

Each `content` item is a ticket summary. No results is successful and returns an empty `content` array.

## 5. Get ticket details

`GET /api/v1/tickets/{ticketId}`

Success: `200 OK` with ticket detail.

Errors:

- `400` for malformed UUID.
- `404` when the ticket does not exist.

## 6. Update ticket fields

`PATCH /api/v1/tickets/{ticketId}`

Request may contain any subset of:

```json
{
  "title": "Updated title",
  "description": "Updated description",
  "priority": "URGENT",
  "assignee": "Avery Singh"
}
```

Rules:

- At least one update field is required.
- Supplied title, description, and priority cannot be null.
- `assignee: null` or a blank assignee clears the assignee.
- `status`, IDs, timestamps, and comments are not accepted.
- Unknown properties and non-writable properties, including `status`, are rejected with `400`; they are never ignored.
- Fields not supplied remain unchanged.
- Updates are allowed in every ticket status.
- Last successful write wins; no version token is required.

Success: `200 OK` with updated ticket detail.

Errors: `400` malformed/unknown/non-writable input; `404` missing ticket; `422` missing fields or field validation.

## 7. Add comment

`POST /api/v1/tickets/{ticketId}/comments`

Request:

```json
{
  "body": "Issue reproduced and under investigation."
}
```

Rules:

- Body is required, trimmed, and 1–2000 characters.
- Comments may be added in every ticket status.

Success:

- `201 Created`
- Body: created comment.

Errors: `400` malformed input; `404` missing ticket; `422` field validation.

Adding a comment does not update the parent ticket's `updatedAt`; the created comment's `createdAt` represents this mutation.

## 8. Transition ticket status

`POST /api/v1/tickets/{ticketId}/transitions`

Request:

```json
{
  "targetStatus": "IN_PROGRESS"
}
```

Rules:

- `targetStatus` is required.
- The complete rules in `state-machine.md` apply.
- This is the only public API operation that changes status.

Success: `200 OK` with updated ticket detail.

Errors:

- `400` malformed UUID, malformed JSON, unknown properties, or unknown status.
- `422` missing, null, blank, or whitespace-only `targetStatus`.
- `404` missing ticket.
- `409` recognized but disallowed current-target transition.

## 9. Problem response

All errors use this extension of Spring `ProblemDetail`:

```json
{
  "type": "https://support-tickets.example/problems/invalid-transition",
  "title": "Invalid ticket status transition",
  "status": 409,
  "detail": "Ticket cannot transition from CLOSED to OPEN.",
  "instance": "/api/v1/tickets/c06571ea-3e71-4ec7-b210-10803a04e73a/transitions",
  "code": "INVALID_STATUS_TRANSITION",
  "fieldErrors": []
}
```

`fieldErrors` is always an array. Validation errors contain entries:

```json
{
  "field": "title",
  "message": "must contain between 1 and 200 characters"
}
```

Stable error codes:

- `MALFORMED_REQUEST`
- `VALIDATION_FAILED`
- `TICKET_NOT_FOUND`
- `INVALID_STATUS_TRANSITION`
- `INTERNAL_ERROR`

Unexpected errors return a generic `500` detail and never expose stack traces, SQL, class names, or secrets.

Code-to-status mapping:

- `MALFORMED_REQUEST` → `400` for malformed JSON/UUID/query values, unknown enum values, unknown properties, or non-writable properties.
- `VALIDATION_FAILED` → `422` for parseable JSON with missing, null, blank, out-of-range, or otherwise invalid fields.
- `TICKET_NOT_FOUND` → `404`.
- `INVALID_STATUS_TRANSITION` → `409`.
- `INTERNAL_ERROR` → `500`.

## 10. Validation status policy

- Use `400 Bad Request` when the request cannot be parsed or a path/query/enum value is malformed.
- Use `400 Bad Request` for a negative/non-integer page, size outside 1–100, unknown query parameters, unknown JSON properties, or non-writable properties.
- Use `422 Unprocessable Entity` when JSON is parseable but a required field is missing/null or a field violates constraints.
- Use `404 Not Found` for an absent ticket.
- Use `409 Conflict` for a valid status name that is not allowed from the ticket's current status.

## 11. Contract invariants

- Create never accepts status.
- Generic update never accepts status.
- Invalid transitions perform no update.
- Successful ticket creation, field updates, and status transitions set or update ticket `updatedAt` and survive restart.
- Comment creation does not update the parent ticket's `updatedAt`.
- List responses never expose comments or full descriptions.
- Detail responses always include `comments`, including an empty array.
