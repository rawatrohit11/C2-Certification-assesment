# Tasks

**Status:** Active  
**Plan:** `docs/plan.md`  
**Last updated:** 2026-09-23

Work top to bottom. Do not start a later phase while its dependencies are open. Implementation tasks are blocked until Phase 0 is complete.

## Phase 0 — Finish specification

- [x] **T-001** Revise `spec/requirements.md` from the 2026-09-23 spec review: remove or qualify “history”; label derived rules; map `AC-015`; map FR-008 to `AC-011`; add `AC-013` to FR-001 and FR-002.  
  Depends on: none. Covers: review findings.

- [x] **T-002** Close `spec/requirements.md` section 11 decisions in writing (frontend, persistence profile, priority, assignee, field limits, comments, search, list ordering, API shape, timestamps, concurrency, terminal-ticket edits, create-time status).  
  Depends on: T-001. Covers: AC-004, AC-005, AC-007, AC-012.

- [x] **T-003** Write `spec/state-machine.md` with the full allowed matrix, terminal states, self-transitions, and rejected examples from `doc/Assessments.pdf` plus `RESOLVED → CANCELLED`, `IN_PROGRESS → OPEN`, `IN_PROGRESS → CLOSED`, and `OPEN → CLOSED`.  
  Depends on: T-002. Covers: AC-009, AC-010, AC-014.

- [x] **T-004** Write `spec/data-model.md` for tickets and comments.  
  Depends on: T-002. Covers: FR-001, FR-004, FR-005, FR-009.

- [x] **T-005** Write `spec/api-contract.md` for create, list, get, update fields, comments, search, status filter, combined query, status transition, and error payloads.  
  Depends on: T-003, T-004. Covers: FR-001 to FR-011, AC-012.

- [x] **T-006** Write `spec/architecture.md` for backend/frontend boundaries, persistence, and where the state machine is enforced.  
  Depends on: T-005. Covers: delivery constraints.

- [x] **T-007** Write `spec/ui-flow.md` for create, list, details, update, assignee, comments, search, filter, transitions, empty states, and error states.  
  Depends on: T-005. Covers: AC-001 to AC-008, AC-013.

- [x] **T-008** Write `spec/test-strategy.md` with unit vs integration split, state-machine cases, restart persistence, and commands to run.  
  Depends on: T-003, T-005, T-007. Covers: AC-014, AC-011.

- [x] **T-009** Run `/review-spec` on the full `spec/` folder and fix blocking findings before Phase 1.  
  Depends on: T-001 to T-008.

## Phase 1 — Foundation

Blocked until T-009.

- [x] **T-010** Create backend and frontend project skeletons matching `architecture.md` (Java 21, Spring Boot, chosen frontend). No feature code.  
  Covers: technical constraints.

- [x] **T-011** Add `.gitignore`, environment samples without secrets, and database configuration for the chosen durable runtime.  
  Covers: AC-015, AC-011.

- [x] **T-012** Create the schema from `data-model.md`.  
  Covers: FR-009.

## Phase 2 — Ticket core

- [x] **T-013** Implement create ticket with backend validation and server-assigned `OPEN` status. Tests for success and validation failure.  
  Covers: FR-001, AC-001, AC-012.

- [x] **T-014** Implement list and get-by-id, including empty list and not-found.  
  Covers: FR-002, FR-003, AC-002, AC-003.

- [x] **T-015** Implement update of title, description, priority, and assignee without accepting status through this path.  
  Covers: FR-004, AC-004, AC-005.

## Phase 3 — Comments, search, filter

- [x] **T-016** Implement add-comment and include comments on ticket details.  
  Covers: FR-005, AC-006.

- [x] **T-017** Implement keyword search per `api-contract.md`.  
  Covers: FR-006, AC-007.

- [x] **T-018** Implement status filter, unknown-status rejection, and combined search-and-filter if the spec requires it.  
  Covers: FR-007, AC-008, AC-012.

## Phase 4 — State machine

- [x] **T-019** Implement the transition use case using the matrix in `state-machine.md`.  
  Covers: FR-008, AC-009.

- [x] **T-020** Reject invalid transitions, including PDF examples, without changing persisted state. Integration tests must pass.  
  Covers: AC-010, AC-014.

## Phase 5 — Frontend

- [x] **T-021** Build list, create, details, update, assignee, comments, search, status filter, pagination, and valid status-transition controls from `ui-flow.md`.  
  Covers: AC-001 to AC-009.

- [x] **T-022** Show meaningful errors for validation, not-found, invalid transitions, connectivity, and unexpected server errors. Failed actions must not appear successful.  
  Covers: FR-011, AC-013.

## Phase 6 — Verification

- [x] **T-023** Run `/generate-tests` against remaining gaps; add any missing state-machine, validation, search, and filter cases.  
  Covers: AC-014, AC-012.

- [x] **T-024** Prove `AC-011` by restarting the backend against PostgreSQL and verifying persisted tickets, updates, status, assignee, and comments. H2 does not satisfy this check.  
  Evidence: `docs/t-024-postgresql-restart.md`.

- [x] **T-025** Scan the repository for secrets and confirm none are committed.  
  Covers: AC-015.

## Phase 7 — Review and fix

- [x] **T-026** Run `/review-code` on implementation changes. Fix blocking findings.

- [x] **T-027** Re-run `/review-spec` if implementation forced a spec change; update affected spec files in the same change.  
  No spec change was required; implementation was aligned to the existing contract.

- [x] **T-028** Confirm prompt history and `docs/ai-validation-log.md` are current.

## Evidence tasks (ongoing)

- [x] **T-029** Append every user prompt to `.specstory/history/prompts.jsonl` and `docs/prompt-history.md` until project hooks are allowed.  
  Current through Phase 7; continue manually if more prompts arrive before hooks are permitted.

- [x] **T-030** Record at least one additional meaningful AI correction during implementation or testing.

- [x] **T-031** Record where GitHub Copilot was used and how its suggestions were validated; do not claim use without evidence.

- [x] **T-032** Evaluate one verified token-optimization plugin or MCP integration (for example codebase-memory). Record the decision, permissions, and evidence; do not install an unverified integration solely for appearance.
