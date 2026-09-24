# Support Ticket Management System

Spec-driven Java 21, Spring Boot, PostgreSQL, and React/Vite assessment project.

## Current phase

Phase 7 complete, including T-024. The API, UI, reviews, evidence pack, and PostgreSQL restart persistence proof are in place.

## Prerequisites

- Java 21
- Maven 3.9+ (use the project wrapper once generated)
- Node.js 22.12+
- npm
- Docker with Compose, or PostgreSQL 18

The repository pins Java and Node major versions in `.java-version` and `.nvmrc`.

## Local configuration

1. Copy `.env.example` to `.env`.
2. Set local database credentials in `.env`. Never commit that file.
3. Export `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and other required values before starting the backend.

Start PostgreSQL:

```bash
docker compose up -d postgres
```

## Backend

```bash
cd backend
JAVA_HOME=/path/to/java-21 ./mvnw test
JAVA_HOME=/path/to/java-21 ./mvnw spring-boot:run
```

The backend uses PostgreSQL by default and H2 only for automated tests. Flyway applies migrations from `backend/src/main/resources/db/migration`.
The durable restart verification procedure is documented in `docs/postgresql-restart-check.md`.

## Frontend

```bash
cd frontend
npm ci
npm run test:run
npm run lint
npm run build
npm run dev
```

The frontend requires Node.js 22.12 or newer. Its development server uses port 5173 and loads `VITE_API_BASE_URL` from the repository-root `.env`.

## Specifications and workflow

- Product and technical specifications: `spec/`
- Plan and task tracking: `docs/plan.md`, `docs/tasks.md`
- Prompt evidence: `.specstory/history/`, `docs/prompt-history.md`
- AI validation evidence: `docs/ai-validation-log.md`

Use `/review-spec`, `/review-code`, and `/generate-tests` at the corresponding workflow gates.
