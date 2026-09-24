# AI Validation Log

This log records AI output that was checked and corrected rather than accepted blindly.

## 2026-09-22 — Repository inventory was overstated

- **AI suggestion:** The initial assessment summary said the workspace was otherwise empty after a broad file search returned no matches.
- **Why it was incorrect:** `doc/Assessments.pdf` was known to exist and had just been read. A zero-result search did not justify a claim about the whole workspace, particularly when ignore rules can hide files.
- **Evidence:** Successful extraction of all five pages from `doc/Assessments.pdf`.
- **Correction:** Treat search results as scoped evidence, preserve known files, and verify targeted paths before making repository-wide claims.
- **Impact:** Prevents accidental assumptions that could overwrite or disregard existing assessment artifacts.

## 2026-09-22 — Legacy commands were not sufficient as the primary workflow

- **AI suggestion:** Mirror the PDF example using only Markdown files under `.cursor/commands/`.
- **Why it was incomplete:** Current Cursor documentation treats commands as a legacy format and recommends Agent Skills for new reusable workflows.
- **Evidence:** Cursor Skills documentation describes `/migrate-to-skills` and `disable-model-invocation: true` for explicit workflows.
- **Correction:** Added current project skills for documentation, code review, specification review, and test generation while retaining the three command files for assessment compatibility.
- **Impact:** The repository remains compatible with the requested evidence structure without depending solely on a deprecated mechanism.

## 2026-09-23 — Plan/Tasks must not skip remaining specifications

- **Phase:** Plan / Tasks
- **AI suggestion:** Treat `spec/requirements.md` as enough specification and start implementation planning as if architecture, API, data model, UI, and tests were already defined.
- **Why it was incorrect:** `doc/Assessments.pdf` requires the full `spec/` set before implementation. `/review-spec` already found those files missing and several acceptance criteria unmeasurable.
- **Evidence:** Assessment Spec Artefacts list; `spec/` contained only `requirements.md`; open decisions in section 11.
- **Correction:** Write `docs/plan.md` and `docs/tasks.md` with Phase 0 as a hard gate: finish specification, then foundation and feature work.
- **Impact:** Prevents coding against unresolved validation, search, and state-machine details.

## 2026-09-23 — Generated contracts left mutation and protected-field behavior ambiguous

- **Phase:** Specification review
- **AI suggestion:** Mark the generated specification pack “approved for implementation” while saying only that successful mutations update ticket `updatedAt` and that protected request fields are “not accepted.”
- **Why it was incorrect:** The comment endpoint returned only a comment, so it was unclear whether the parent ticket timestamp changed. “Not accepted” also failed to define whether `status` or unknown fields were ignored or rejected. Backend, UI, and tests could implement incompatible behavior.
- **Evidence:** Cross-document review of `data-model.md`, `api-contract.md`, `ui-flow.md`, and `test-strategy.md`.
- **Correction:** Comment creation does not change parent `updatedAt`; unknown and non-writable properties are rejected with `400 MALFORMED_REQUEST`; required-field violations use `422 VALIDATION_FAILED`. UI and test behavior were aligned.
- **Impact:** Removes status-smuggling ambiguity and gives comment timestamps, errors, and integration tests one deterministic contract.

## 2026-09-23 — Persistent terminal directory invalidated dependency checks

- **Phase:** Phase 1 foundation
- **AI suggestion:** Treat `working_directory` on later terminal calls as proof that npm and Maven were executing inside `frontend/` and `backend/`.
- **Why it was incorrect:** The persistent terminal retained the repository-root directory. npm reported “up to date” without reading `frontend/package.json`, and Maven wrapper attempts did not initially run against `backend/pom.xml`.
- **Evidence:** `pwd` and `npm prefix` both returned the repository root; `npm pkg get` reported that no package file existed there.
- **Correction:** Prefix project-specific terminal commands with an explicit `cd frontend` or `cd backend`, then verify the tool sees the expected manifest.
- **Impact:** Prevents false validation results and ensures generated lockfiles, wrappers, and build output belong to the correct project.

## 2026-09-23 — Test configuration silently weakened the API contract

- **Phase:** Phase 2 ticket core
- **AI suggestion:** Rely on the strict unknown-property setting in the main application configuration when testing protected create and update fields.
- **Why it was incorrect:** The test-specific `application.yml` replaced the main configuration document and omitted that Jackson setting. Requests containing `status` were silently accepted or treated as empty updates, so two contract tests failed.
- **Evidence:** The first Phase 2 test run returned `201` for create with `status` and `422` for update with only `status`, instead of `400 MALFORMED_REQUEST`.
- **Correction:** Added `spring.jackson.deserialization.fail-on-unknown-properties: true` to the test configuration and retained regression tests for protected fields.
- **Impact:** Test and runtime profiles now enforce the same protected-field and unknown-property contract.

## 2026-09-23 — Code review found ignored query-parameter typos

- **Phase:** Phase 2 review and Phase 3 implementation
- **AI suggestion:** Treat the Phase 2 list endpoint as contract-complete because page and size validation passed.
- **Why it was incomplete:** Spring MVC silently ignores undeclared query parameters. A typo such as `szie=10` therefore returned `200` with default pagination even though the API contract requires unknown query parameters to return `400 MALFORMED_REQUEST`.
- **Evidence:** `/review-code` identified that `TicketController.list` declared only individual parameters and had no allow-list check.
- **Correction:** Phase 3 now captures all list query parameters, rejects names outside `keyword`, `status`, `page`, and `size`, and includes a regression test for the typo case.
- **Impact:** Misspelled filters and pagination controls no longer produce misleading successful responses.

## 2026-09-23 — Frontend API base path initially duplicated the version prefix

- **Phase:** Phase 5 frontend
- **AI suggestion:** Define each frontend request with `/api/v1` while also prefixing it with `VITE_API_BASE_URL`.
- **Why it was incorrect:** The existing `.env.example` defines `VITE_API_BASE_URL=http://localhost:8080/api/v1`. Combining both values would request `/api/v1/api/v1/tickets`. Vite also did not yet load the repository-root environment file.
- **Evidence:** Cross-checking `frontend/src/api/tickets.ts`, `.env.example`, and Vite's environment-directory behavior during the Phase 5 review.
- **Correction:** Request functions now use paths relative to an API base that defaults to `/api/v1`, and `vite.config.ts` loads environment values from the repository root.
- **Impact:** Both configured local development and same-origin deployments target the documented API paths.

## 2026-09-23 — Generated boundary tests used an unsupported JSONPath function

- **Phase:** Phase 6 generated tests
- **AI suggestion:** Assert exact string boundaries using expressions such as `$.title.length()` in MockMvc JSONPath checks.
- **Why it was incorrect:** The configured JSONPath implementation returned null for string `length()` expressions, causing five false failures even though the API returned the exact accepted values.
- **Evidence:** The first focused `TicketValidationIntegrationTests` run reported 24 tests with five failures, all at string-length JSONPath assertions.
- **Correction:** Parse successful response JSON with the configured Jackson mapper and assert string sizes directly with AssertJ. The focused suite then passed all 24 tests.
- **Impact:** Boundary tests now verify observable values without depending on unsupported JSONPath behavior.

## 2026-09-23 — Review findings treated omitted assignee as success

- **Phase:** Phase 7 review and fix
- **AI suggestion:** Treat a missing `assignee` JSON property as the cleared-assignee success case.
- **Why it was incorrect:** `spec/api-contract.md` shows `"assignee": null`. Omitting the field made tests pass while the published contract still promised an explicit null.
- **Evidence:** `/review-code` compared the contract example with `jsonPath("$.assignee").doesNotExist()`.
- **Correction:** Enabled `spring.jackson.default-property-inclusion: always` and asserted `null`.
- **Impact:** Unassigned tickets now match the documented representation.

## 2026-09-23 — GitHub Copilot usage was not evidenced

- **Phase:** Phase 7 evidence (T-031)
- **AI suggestion:** The assessment asks for GitHub Copilot as a required development tool, so it could be tempting to claim Copilot assistance during implementation.
- **Why that would be incorrect:** This session used Cursor Agent only. No Copilot suggestion, accept, or reject event is recorded in the workspace evidence.
- **Evidence:** Prompt history, AI validation log, and absence of Copilot-specific artifacts.
- **Correction:** Record that Copilot was not observed and must not be claimed.
- **Impact:** Assessment evidence stays limited to tools that were actually used.

## 2026-09-24 — PostgreSQL 18 rejected the legacy data mount

- **Phase:** T-024 PostgreSQL restart proof
- **AI suggestion:** Keep the Compose volume at `/var/lib/postgresql/data`, the historical PostgreSQL Docker path.
- **Why it was incorrect:** `postgres:18.6-alpine` stores cluster data under `/var/lib/postgresql/<major>` and refuses to start when a volume is mounted at the unused `/var/lib/postgresql/data` path.
- **Evidence:** Container exit code 1 and the official PostgreSQL 18 Docker image error about an unused data mount.
- **Correction:** Mount the named volume at `/var/lib/postgresql`.
- **Impact:** The durable runtime can start and AC-011 can be proven against PostgreSQL.
