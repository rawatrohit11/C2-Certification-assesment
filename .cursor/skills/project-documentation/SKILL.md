---
name: project-documentation
description: Creates and updates project specifications, architecture notes, API documentation, setup guides, and decision records. Use when documenting requirements, design, behavior, testing, setup, or AI validation evidence.
---

# Project Documentation

1. Identify the document's audience, purpose, and authoritative inputs.
2. Read related specifications and implementation before describing existing behavior.
3. Separate confirmed facts, decisions, assumptions, and unresolved questions.
4. Use the domain vocabulary and status names from `spec/requirements.md` and `spec/state-machine.md`.
5. Keep cross-document links and acceptance-criteria traceability accurate.
6. Prefer concise prose, examples, and Mermaid diagrams over duplicated implementation details.
7. Check all commands, paths, configuration names, and API examples against the repository.
8. Never include secrets or claim a check passed unless it was run.

For an AI correction entry in `docs/ai-validation-log.md`, record:

- Date and workflow phase
- Original suggestion
- Why it was incorrect or unsafe
- Evidence used to identify the issue
- Accepted correction and impact
