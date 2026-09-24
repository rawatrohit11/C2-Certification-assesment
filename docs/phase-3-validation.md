# Phase 3 Validation

**Date:** 2026-09-23  
**Scope:** Comments, search, and filtering (T-016–T-018)

## Implemented behavior

- Add validated, trimmed comments to existing tickets.
- Return ticket comments oldest-first by creation time and identifier.
- Keep the parent ticket `updatedAt` unchanged when comments are added.
- Search title and description with trimmed, case-insensitive substring matching.
- Treat `%` and `_` as literal search text rather than client-controlled SQL wildcards.
- Filter by ticket status and combine status and keyword using AND semantics.
- Preserve newest-first pagination for filtered and unfiltered results.
- Reject unknown status values and unknown query parameter names with `400 MALFORMED_REQUEST`.

## Verification

Command:

```bash
cd backend
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw test
```

Result:

- Build succeeded on Java 21 and Spring Boot 4.1.1.
- 23 tests passed: 2 foundation tests and 21 ticket API integration tests.
- Phase 3 coverage includes comment success, trimming, ordering, validation, missing tickets, unchanged parent timestamps, title and description search, case-insensitivity, literal wildcard characters, blank keywords, status filtering, combined filtering, no matches, unknown statuses, and unknown query parameters.
- IDE diagnostics reported no errors in backend source or tests.

## Review notes

- Comments use their own entity and repository while retaining the migration-defined foreign key.
- Ticket detail performs one ordered comment query; list responses do not load or expose comments.
- Spring Data `ContainingIgnoreCase` queries provide parameter binding and wildcard escaping; no query text is built from raw client input.
- The Phase 2 review finding about silently ignored query parameters is resolved.

## Deferred checks

- PostgreSQL-specific case-insensitive query compatibility and durable restart verification remain Phase 6 work.
- Full validation-boundary generation remains T-023.
- Status-transition behavior begins in Phase 4.
- Frontend comment, search, and filter behavior begins in Phase 5.
