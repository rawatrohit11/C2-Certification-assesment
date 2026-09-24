# PostgreSQL Restart Persistence Check

**Purpose:** Prove AC-011 against durable PostgreSQL storage. In-memory H2 does not satisfy this check.

## Prerequisites

1. Copy `.env.example` to the ignored `.env` file.
2. Set a local PostgreSQL password in `.env`.
3. Export `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` into the backend process environment without printing their values.
4. Start PostgreSQL:

```bash
docker compose up -d postgres
docker compose ps
```

Wait until the container reports `healthy`.

PostgreSQL 18 images must mount the named volume at `/var/lib/postgresql`, not `/var/lib/postgresql/data`.

## First backend run

Start the backend:

```bash
cd backend
JAVA_HOME=/path/to/java-21 ./mvnw spring-boot:run
```

From a separate terminal, create a ticket and retain the returned `id`:

```bash
curl --fail-with-body \
  -H 'Content-Type: application/json' \
  -d '{"title":"Restart persistence","description":"Durable PostgreSQL check","priority":"HIGH","assignee":"Persistence Tester"}' \
  http://localhost:8080/api/v1/tickets
```

Using that identifier:

1. PATCH the title, description, priority, and assignee.
2. Add a comment.
3. Transition `OPEN → IN_PROGRESS → RESOLVED`.
4. GET the detail and record the non-secret response as pre-restart evidence.

## Restart and verification

Stop only the backend process. Do not stop PostgreSQL or remove its volume. Start the backend again with the same environment, then request:

```bash
curl --fail-with-body \
  http://localhost:8080/api/v1/tickets/{ticketId}
```

Confirm that the response retains:

- The same ticket identifier.
- Updated title, description, priority, and assignee.
- `RESOLVED` status.
- The previously added comment.
- Original creation timestamp and persisted update timestamp.

Also confirm the ticket appears through list, keyword search, and `status=RESOLVED` filtering.

## Evidence to record

- PostgreSQL container health before both backend runs.
- Flyway reporting schema version `1` without recreating the schema on restart.
- Sanitized pre-restart and post-restart ticket responses.
- Backend stop and second startup timestamps.

Never commit `.env`, command history containing credentials, database dumps with credentials, or raw environment output.
