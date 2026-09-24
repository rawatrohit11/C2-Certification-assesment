# Test Strategy

**Status:** Approved for implementation  
**Sources:** all specification files in `spec/`  
**Last updated:** 2026-09-23

## 1. Goals

- Prove each core acceptance criterion with executable checks or an explicit durable-runtime procedure.
- Test the backend as the authority for validation and state transitions.
- Catch contract drift between backend and frontend.
- Keep tests deterministic, isolated, and understandable.

## 2. Test layers

### Backend domain unit tests

Test the transition policy without Spring or a database:

- All five allowed edges.
- Every self-transition.
- Every disallowed current-target pair.
- Terminal-state behavior.

These tests are exhaustive over the five-state Cartesian product.

### Backend service unit tests

Use mocks only where they make business outcomes clearer:

- Invalid transitions do not call repository save.
- Create assigns `OPEN` and server values.
- Field update cannot alter status.
- Comment creation verifies ticket existence.

### Backend API integration tests

Use Spring Boot test support and MockMvc or the framework-equivalent HTTP test client against H2 for portable behavior:

- Create, list, detail, update, comment.
- Search, status filter, combined filtering, ordering, and pagination.
- Required fields, boundaries, malformed UUIDs/enums, and not-found.
- Unknown and non-writable JSON properties are rejected rather than ignored.
- Problem content type, stable code, status, detail, and field errors.
- Allowed and invalid transitions with persisted-state assertions.

Do not mock the transition policy, application service, or repository in these integration tests.

### PostgreSQL integration and restart checks

Use the runtime PostgreSQL profile for:

- Migration execution.
- Enum-string, timestamp, and query compatibility.
- Case-insensitive substring search behavior.
- AC-011 durable restart procedure.

H2 success does not replace these checks.

### Frontend component and integration tests

Use the React/Vite test stack selected during scaffolding, with network calls replaced by controlled API responses:

- Create form validation and successful navigation.
- List loading, database-empty, no-match, populated, and request-error states.
- Search/filter query composition and pagination retention.
- Detail loading, success, and not-found.
- Field update success and backend field errors.
- Add-comment success and retained body on failure.
- Adding a comment leaves the parent ticket `updatedAt` unchanged.
- Transition actions by current status.
- `409` transition handling and reload.
- Connectivity and unexpected-error messages.

Test observable UI behavior; do not assert component internals.

### End-to-end smoke check

After backend and frontend are running against PostgreSQL:

1. Create a ticket in the UI.
2. Find it in the list.
3. Open details and update fields and assignee.
4. Add a comment.
5. Find it through keyword search and status filter.
6. Perform `OPEN → IN_PROGRESS → RESOLVED → CLOSED`.
7. Confirm an invalid terminal transition is rejected through a direct API call.
8. Restart the backend and confirm ticket, updates, status, and comment remain.

Automating this smoke check is optional unless repeatability or CI requirements make an end-to-end dependency worthwhile.

## 3. Required state-machine integration cases

Successful:

- `OPEN` → `IN_PROGRESS`
- `OPEN` → `CANCELLED`
- `IN_PROGRESS` → `RESOLVED`
- `IN_PROGRESS` → `CANCELLED`
- `RESOLVED` → `CLOSED`

Rejected assessment examples:

- `CLOSED` → `OPEN`
- `RESOLVED` → `OPEN`
- `CANCELLED` → `OPEN`

Additional rejected coverage:

- `OPEN` → `RESOLVED`
- `OPEN` → `CLOSED`
- `IN_PROGRESS` → `OPEN`
- `IN_PROGRESS` → `CLOSED`
- `RESOLVED` → `IN_PROGRESS`
- `RESOLVED` → `CANCELLED`
- One self-transition for each status.

Every rejected case asserts HTTP `409`, problem code `INVALID_STATUS_TRANSITION`, and unchanged persisted status. Unknown target values assert `400`, not `409`.

## 4. Validation boundaries

Test each text field at:

- Null when required.
- Empty and whitespace-only.
- Minimum valid content.
- Exact maximum length.
- Maximum plus one.
- Leading and trailing whitespace normalization.

Also test:

- Every valid priority and one unknown value.
- Assignee null, blank-to-null, maximum, and too long.
- Patch with no fields.
- Patch attempt containing status.
- Page below zero, size below one, and size above 100.

Expected responses:

- Unknown/non-writable properties, malformed UUIDs/enums, and invalid pagination use `400` with `MALFORMED_REQUEST`.
- Missing/null required JSON fields and length/range violations use `422` with `VALIDATION_FAILED`.
- A PATCH containing `status` uses `400` and leaves status unchanged.
- An empty PATCH uses `422` and changes nothing.

## 5. Search and filter coverage

- Keyword matches title only.
- Keyword matches description only.
- Match is case-insensitive.
- Partial substring matches.
- Blank keyword acts as no keyword filter.
- Valid status filter.
- Unknown status rejected.
- Keyword and status use AND semantics.
- Empty results are successful.
- Newest-first ordering is deterministic.
- Page metadata and maximum size are correct.
- Invalid page and size values return `400` with `MALFORMED_REQUEST`.

## 6. Acceptance-criteria evidence

- AC-001: create API integration plus create UI test and smoke check.
- AC-002: list API and UI tests.
- AC-003: detail API and UI tests.
- AC-004 and AC-005: patch API and edit UI tests.
- AC-006: comment API and UI tests.
- AC-007 and AC-008: search/filter API and UI tests.
- AC-009, AC-010, and AC-014: state-machine unit and integration suites.
- AC-011: PostgreSQL restart procedure.
- AC-012: validation integration suite.
- AC-013: UI error-state tests.
- AC-015: repository scan plus review of tracked configuration.

Terminal-status integration coverage must prove both sides of the approved rule:

- No status transition succeeds from `CLOSED` or `CANCELLED`.
- Field updates and comment creation still succeed in both terminal statuses.

## 7. Planned commands

These commands become executable after Phase 1 creates wrappers and manifests:

```bash
cd backend && ./mvnw test
cd frontend && npm test -- --run
cd frontend && npm run lint
cd frontend && npm run build
```

Run focused tests during development, then all commands before review. Record commands actually run and failures; never mark a check passed because it is planned.

The durable restart procedure must also be documented in the setup guide after runtime configuration exists.

## 8. Test data

- Generate UUIDs unless a stable ID clarifies an assertion.
- Use a controllable clock for timestamp assertions.
- Build tickets through shared test builders that require title, description, priority, and status explicitly.
- Reset state between integration tests.
- Never depend on test order or external network services.
- Do not use production credentials or copied customer data.

## 9. Exit criteria

- All required commands pass.
- Every acceptance criterion has evidence listed in section 6.
- State-machine integration cases pass.
- PostgreSQL restart persistence passes.
- No skipped or quarantined test hides a required behavior.
- Remaining gaps and unexecuted checks are explicitly reported.
