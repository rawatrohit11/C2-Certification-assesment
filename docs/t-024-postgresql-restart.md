# T-024 PostgreSQL Restart Evidence

**Date:** 2026-09-24  
**Acceptance criterion:** AC-011  
**Database:** PostgreSQL 18.6 via `compose.yaml`  
**Volume:** `c2-assessment-cursor_support-ticket-postgres` mounted at `/var/lib/postgresql`

## Runtime notes

- Local credentials lived only in the ignored `.env` file. They are not recorded here.
- Port 8080 was already bound by an unrelated listener, so both backend runs used `SERVER_PORT=18080`. Persistence is independent of the HTTP port.
- PostgreSQL stayed healthy across backend stop and start. The container was not removed and the named volume was not deleted after the first successful init.

## Compose correction

`postgres:18.6-alpine` refused the legacy `/var/lib/postgresql/data` mount. The volume was remounted at `/var/lib/postgresql` before the proof run.

## First durable backend run

PostgreSQL health: `healthy`.

Flyway on the first process that reached a running HTTP server:

- Database: `jdbc:postgresql://localhost:5432/support_tickets` (PostgreSQL 18.6)
- Current schema version: `1`
- `Schema "public" is up to date. No migration necessary.`

(The schema itself was created earlier in the same volume when Flyway applied `V1` before a failed bind on port 8080.)

Seeded ticket `7dc01e1e-bcee-4521-9d16-2bc11abb5aa3`:

1. Created with title `Restart persistence`, HIGH priority, assignee `Persistence Tester`.
2. PATCH to title `Restart persistence updated`, description `Durable PostgreSQL check after patch`, priority `URGENT`, assignee `Persistence Reviewer`.
3. Comment `Issue reproduced and under investigation.` (`85ac4fa0-dcce-4325-937c-6f7c2fc0164e`).
4. Transitions `OPEN → IN_PROGRESS → RESOLVED`.

Pre-restart detail (non-secret):

```json
{
  "id": "7dc01e1e-bcee-4521-9d16-2bc11abb5aa3",
  "title": "Restart persistence updated",
  "description": "Durable PostgreSQL check after patch",
  "priority": "URGENT",
  "status": "RESOLVED",
  "assignee": "Persistence Reviewer",
  "createdAt": "2026-09-24T04:43:17.168016Z",
  "updatedAt": "2026-09-24T04:43:17.739924001Z",
  "comments": [
    {
      "id": "85ac4fa0-dcce-4325-937c-6f7c2fc0164e",
      "body": "Issue reproduced and under investigation.",
      "createdAt": "2026-09-24T04:43:17.695995Z"
    }
  ]
}
```

Backend stop: process on port 18080 terminated. PostgreSQL remained `healthy`.

## Second backend run

PostgreSQL health before the second start: `healthy`.

Flyway:

- Current schema version: `1`
- `Schema "public" is up to date. No migration necessary.`

Started `SupportTicketApplication` at 2026-09-24T10:13:33.789+05:30.

Post-restart `GET /api/v1/tickets/7dc01e1e-bcee-4521-9d16-2bc11abb5aa3` returned the same identifier, updated fields, `RESOLVED` status, assignee, comment id/body/`createdAt`, and original `createdAt`. `updatedAt` serialized as `2026-09-24T04:43:17.739924Z` (PostgreSQL timestamp precision dropped the extra nanosecond from the in-memory pre-restart JSON).

After a further backend restart against the same volume:

- List `page=0&size=20` included the ticket (`totalElements` 1).
- `keyword=persistence` included the ticket.
- `status=RESOLVED` included the ticket.

## Result

AC-011 is proven against durable PostgreSQL. H2 was not used for this check.
