---
name: generate-tests
description: Generates focused tests from specifications and acceptance criteria using existing project conventions. Use when the user explicitly invokes /generate-tests.
disable-model-invocation: true
---

# Generate Tests

1. Read `spec/test-strategy.md`, related acceptance criteria, implementation, and existing tests.
2. Build a coverage list before editing: happy paths, boundaries, failures, and regressions.
3. Reuse the repository's test framework, fixtures, naming, and setup; add no dependency unless necessary.
4. Generate the smallest deterministic tests that verify observable behavior.
5. For state transitions, cover every allowed edge and representative rejected edges from each terminal state.
6. For API behavior, verify status, content type, response shape, validation details, and persisted result.
7. Do not weaken assertions or change production behavior merely to make a test pass.
8. Run the narrowest relevant test command, then the broader suite when practical.
9. Report tests added, requirements covered, commands run, and remaining gaps.
