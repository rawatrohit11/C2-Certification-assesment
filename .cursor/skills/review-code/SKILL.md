---
name: review-code
description: Reviews implementation changes against project specifications and engineering standards. Use when the user explicitly invokes /review-code.
disable-model-invocation: true
---

# Review Code

1. Read the relevant specifications, changed files, and tests.
2. Review the diff for correctness, regressions, security, validation, data integrity, and maintainability.
3. Verify backend enforcement of ticket state transitions and API error semantics.
4. Check persistence behavior, frontend error handling, and acceptance-criteria coverage where relevant.
5. Run focused tests or static checks when available; do not claim unexecuted checks passed.
6. Report findings first, ordered by severity, with file and line references.
7. For each finding, explain the failure scenario and smallest viable correction.
8. End with test gaps, unresolved assumptions, and a concise verdict.

Do not edit code unless the user also asks for fixes.
