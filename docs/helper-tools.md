# Helper Tools

The project keeps reusable AI guidance in the repository so every contributor receives the same instructions.

## Persistent rules

- `.cursor/rules/spec-driven-workflow.mdc` — required delivery phases and assessment evidence
- `.cursor/rules/java-spring-boot.mdc` — Java 21 and Spring Boot conventions
- `.cursor/rules/testing.mdc` — unit, integration, state-machine, and UI test standards
- `.cursor/rules/api-standards.mdc` — REST contracts, validation, and error responses
- `.cursor/rules/documentation.mdc` — specification and documentation quality

## Agent Skills

- `/project-documentation` — create or update specifications and project documentation
- `/review-code` — review changes against specifications without editing
- `/review-spec` — review specification completeness and consistency
- `/generate-tests` — generate focused tests from acceptance criteria

The last three are explicit-invocation skills. Matching files under `.cursor/commands/` preserve the command-shaped evidence requested by the assessment and compatibility with Cursor versions that still load project commands.

## Prompt and AI validation evidence

- `.specstory/history/prompts.jsonl` retains exact prompt text.
- `docs/prompt-history.md` is the readable prompt index.
- `docs/ai-validation-log.md` records rejected or corrected AI suggestions and supporting evidence.

Automatic prompt capture is currently unavailable because the active Cursor permissions policy blocks project hook creation. Prompt history must be updated manually until `.cursor/hooks.json` can be explicitly approved.

## Token discipline

- Keep rules focused and scoped by file glob.
- Load detailed workflows as skills only when relevant.
- Search narrowly before reading full files.
- Use focused tests before broad suites.
- Prefer repository evidence over repeatedly restating context in prompts.

No external helper dependency or MCP server is configured yet. Add one only when its purpose, maintenance status, permissions, and repository configuration are verified.
