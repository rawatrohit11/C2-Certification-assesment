---
name: review-spec
description: Reviews specification artifacts for completeness, consistency, feasibility, and acceptance-criteria traceability. Use when the user explicitly invokes /review-spec.
disable-model-invocation: true
---

# Review Specification

1. Read all files under `spec/` plus `doc/Assessments.pdf`.
2. Check requirements for ambiguity, missing actors, constraints, and measurable acceptance criteria.
3. Check consistency across architecture, data model, API contract, state machine, UI flow, and test strategy.
4. Verify every required feature and acceptance criterion is covered by design and tests.
5. Validate the complete transition matrix, including terminal states and invalid transitions.
6. Check validation rules, error semantics, persistence assumptions, and search/filter behavior.
7. Identify premature implementation choices and undocumented decisions.
8. Report findings by severity with exact document references and proposed wording.

Do not alter specifications unless the user also asks for revisions.
