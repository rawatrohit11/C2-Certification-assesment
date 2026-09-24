# UI Flow

**Status:** Approved for implementation  
**Sources:** `spec/requirements.md`, `spec/api-contract.md`, `spec/state-machine.md`  
**Last updated:** 2026-09-23

## 1. UI principles

- Make the current ticket status and available actions clear.
- Keep backend responses authoritative.
- Preserve user-entered form values after a failed submission.
- Distinguish loading, empty, success, validation, not-found, connectivity, and unexpected-error states.
- Never display a failed mutation as successful.
- Use accessible labels, keyboard-reachable controls, and a live announcement area for action feedback.

## 2. Ticket list route

Route: `/tickets`

Displays:

- Page heading and “Create ticket” action.
- Keyword search input.
- Status filter with “All statuses” plus every valid status.
- Ticket summaries: title, priority, status, assignee or “Unassigned”, and creation time.
- Pagination controls and current result information.

Flow:

1. Load page 0 with size 20.
2. Search submission trims the keyword and returns to page 0.
3. Status change returns to page 0.
4. Search and status filter remain active together.
5. Pagination retains active filters.
6. Selecting a ticket opens `/tickets/:ticketId`.

The initial UI uses a fixed page size of 20. It forces and preserves `size=20` in the URL, including after a manually edited URL, and does not expose a page-size selector.

States:

- Loading: show a visible loading state without stale success messages.
- Empty database: “No tickets have been created.”
- No filter match: “No tickets match the current search and filter.”
- Error: show a retry action and safe message; do not show the error as an empty result.

## 3. Create ticket route

Route: `/tickets/new`

Fields:

- Title, required, maximum 200.
- Description, required, maximum 5000.
- Priority, required: `LOW`, `MEDIUM`, `HIGH`, `URGENT`.
- Assignee, optional, maximum 100.

Status is not shown as an editable create field.

Flow:

1. User completes fields and submits.
2. Disable duplicate submission while the request is active.
3. On `201`, navigate to the created ticket detail and announce success.
4. On `422`, show field messages and focus the first invalid field.
5. On `400`, preserve all inputs and show a safe action-level error; this normally indicates UI/API contract drift.
6. On connectivity or unexpected failure, preserve all inputs and show an action-level error.

Client checks mirror basic required and length rules but do not replace backend errors.

## 4. Ticket detail route

Route: `/tickets/:ticketId`

Displays:

- Title, description, priority, assignee, current status.
- Created and updated timestamps.
- Valid transition actions for the current status.
- Comment list oldest-first.
- Add-comment form.
- Link back to the ticket list.

Loading and failures:

- Loading: show detail loading state.
- `404`: show “Ticket not found” and a link to `/tickets`.
- Other failures: show retry and navigation options.

## 5. Edit ticket fields

The detail page exposes editing for title, description, priority, and assignee.

Flow:

1. Start with the latest loaded values.
2. Disable save while no field is dirty; do not send an empty patch.
3. Submit only changed fields through `PATCH`.
4. On success, replace displayed ticket data with the response and announce success.
5. On field validation, retain edits and show field messages.
6. On `400` or another failure, keep the previous confirmed ticket state and the user's draft, and show a safe action-level error.

Editing remains available in `CLOSED` and `CANCELLED`. No edit form contains status.

## 6. Add comment

Flow:

1. User enters a required comment body, maximum 2000.
2. Submit to the comments endpoint.
3. On success, append the returned comment in deterministic order, clear the input, and announce success.
4. On validation or request failure, preserve the text and show a meaningful error.

Comment entry remains available in every status.
Adding a comment does not change the parent ticket's displayed `updatedAt`.

## 7. Status transitions

Show only these actions:

- `OPEN`: “Start progress” and “Cancel ticket”.
- `IN_PROGRESS`: “Resolve ticket” and “Cancel ticket”.
- `RESOLVED`: “Close ticket”.
- `CLOSED`: no status action.
- `CANCELLED`: no status action.

Flow:

1. User selects a transition action.
2. For cancellation or closing, request confirmation to reduce accidental terminal changes.
3. Submit the exact target status to the transition endpoint.
4. Disable transition actions while pending.
5. On success, replace the ticket with the returned detail.
6. On `409`, show the backend detail and reload the ticket because another successful write may have changed its status.
7. On other errors, keep the confirmed local status unchanged and offer retry.

Hiding invalid actions is a usability feature, not a security or validation boundary.

## 8. Problem-response handling

- `VALIDATION_FAILED`: attach `fieldErrors` to matching fields and show the problem detail.
- `TICKET_NOT_FOUND`: render not-found for initial detail load; show an action error if a previously loaded ticket disappears.
- `INVALID_STATUS_TRANSITION`: show the detail near transition controls and reload.
- `MALFORMED_REQUEST`: show a safe action error; this indicates a UI/API integration defect for fixed fields.
- `INTERNAL_ERROR`: show a generic retry message.
- Network failure or unreadable response: “Unable to reach the support service. Check your connection and try again.”

The UI must not render raw stack traces, HTML server errors, or unknown object values.

## 9. Navigation and refresh behavior

- Direct navigation and browser refresh on list, create, and detail routes must work in the configured development/runtime setup.
- Search, status, page, and size are represented in the list URL query string so refresh and back navigation retain them.
- After creation, the detail URL uses the server-issued ticket ID.

## 10. Acceptance-criteria mapping

- Create flow: AC-001, AC-012, AC-013.
- List and pagination: AC-002, AC-013.
- Detail route: AC-003.
- Edit flow: AC-004, AC-005, AC-012, AC-013.
- Comments: AC-006.
- Search and filter: AC-007, AC-008, AC-013.
- Transition controls and errors: AC-009, AC-010, AC-013.
- All successful mutation views rely on persisted backend responses for AC-011.
