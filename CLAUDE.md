# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository layout

- `backend/` — Spring Boot REST API (Java 21). The real, production-bound implementation.
- `frontend/` — React 19 + TypeScript + Vite SPA. The real, production-bound implementation.
- `pipeline-crm-prototype.html` — a standalone, no-backend HTML/CSS/vanilla-JS mockup (all data in memory, resets on reload). It is the original UX/business-logic spec used to design `backend/` and `frontend/`, not code that is deployed or extended. Treat it as read-only design reference.
- `DEVELOPMENT_BACKLOG.md` — authoritative backend spec: roles, per-endpoint rules, sprint breakdown. What `backend/` was built against.
- `FRONTEND_PLAN.md` — frontend technical plan: stack constraints, folder layout, DTO/page contracts. What `frontend/` was built against.
- `DEEPSEEK_BACKEND_IMPLEMENTATION_PROMPT.md` — the original backend implementation prompt.

## Commands

### Backend (run from `backend/`)

```bash
# 1. Postgres (only needed once per machine reboot / volume reset)
docker compose --env-file .env.example up -d
docker compose --env-file .env.example ps    # wait for "healthy"

# 2. Run the app (env vars come straight from .env.example, no .env file needed)
export JAVA_HOME=/opt/homebrew/opt/openjdk@21   # only if `java -version` doesn't already resolve to 21
export PATH="$JAVA_HOME/bin:$PATH"
set -a; source .env.example; set +a
./mvnw spring-boot:run

# Health check
curl http://localhost:8080/api/v1/health   # {"status":"ok"}

# Tests (needs Docker running — Testcontainers spins up Postgres 16)
./mvnw test
./mvnw test -Dtest=BusinessRulesIntegrationTest                 # single class
./mvnw test -Dtest=BusinessRulesIntegrationTest#someMethodName  # single test
```

### Frontend (run from `frontend/`)

```bash
npm run dev      # Vite dev server, http://localhost:5173
npm run build    # tsc -b && vite build
npm run lint      # oxlint
```
No test suite is configured on the frontend yet.

### Local dev gotchas

- This machine has no system `java`; JDK 21 lives at `/opt/homebrew/opt/openjdk@21` (Homebrew). Export `JAVA_HOME`/`PATH` as above before any `./mvnw` command.
- Ports: backend `8080`, frontend `5173` (falls back to `5174` if busy — if so, restart backend with `FRONTEND_ORIGIN=http://localhost:5174 ./mvnw spring-boot:run` so CORS allows it), Postgres `5434` (not the Postgres default 5432, to avoid clashing with other local projects).
- Never run `docker compose down -v` — it deletes the Postgres volume (local data). Changing `POSTGRES_PASSWORD` in the env file does not change the already-provisioned role password inside an existing volume.
- Seed logins: `admin`/`admin123` (ADMIN), `anya.t`/`curator1` and `igor.l`/`curator2` (CURATOR, active), `sveta.r`/`curator3` (CURATOR, blocked).

## Architecture

### Backend — modular monolith, packages under `com.pipeline.crm.*`

`auth`, `security`, `user`, `lead`, `student`, `pipeline` (stages), `cohort`, `statistics`, `audit`, `common`, `config`.

- **Auth**: `POST /api/v1/auth/login` returns a JWT access token (15 min) and sets an HttpOnly refresh cookie (7 days, `POST /auth/refresh` to rotate). `CurrentUser` (id, username, role) is the request-scoped identity used everywhere.
- **RBAC**: ADMIN has full access; CURATOR is scoped to their own leads/students. Fetching another curator's record by ID returns `404`, not `403` (don't leak existence). Server-side checks are the only source of truth — the frontend hiding a button is not enforcement.
- **Schema/migrations**: Flyway-owned; Hibernate runs with `ddl-auto: validate`, so it never creates/alters tables itself. Every schema or data change is a new `V{n}__description.sql` under `backend/src/main/resources/db/migration/` — never edit an already-applied migration. `V1` is the schema, `V2` seeds users/pipeline-stages/cohorts, `V4` adds `students.stage_position` (manual Kanban card order); all three apply unconditionally everywhere (local, tests, prod). `V3` (realistic 6-months-of-cohorts lead/student demo data) lives in a separate `backend/src/main/resources/db/dev-seed/` location that's only added to `spring.flyway.locations` under the `dev` Spring profile (`application-dev.yml`), activated locally via `SPRING_PROFILES_ACTIVE=dev` in `.env.example` — **never** activate `dev` in production, `V3` deletes all existing leads/students before inserting its demo set. (Its header also documents the id-prefix convention needed to keep hand-authored UUIDs valid: only hex digits are legal in a UUID literal — letters like `g`/`h` are not.)
- **Kanban card order**: `Student.stagePosition` is a plain integer, scoped per `currentStageId`, used only as a tie-breaker *within* a zone — it does not encode the red/normal/paused grouping itself (that's always computed live, see `cardZone.ts` / `HealthCalculator`). `POST /students/{id}/reorder` (`{stageId, beforeStudentId}`) drives the drag gesture: on every call it re-fetches all siblings in the target stage ordered by `stagePosition`, splices the moved student in at the `beforeStudentId` anchor (or appends if `null`), and renumbers that stage's siblings 0..n-1 in one transaction — simple and correct at this app's scale (≤ a few dozen students per stage), not something to over-engineer with fractional positions. Positions are intentionally allowed to go non-dense across stages (e.g. a stage a student just left keeps its old gaps) — nothing relies on density, only relative order, and `StudentRepository.nextStagePosition` always uses `MAX+1` so it's gap-safe.
- **Core domain flow**: `Lead` → (`POST /leads/{id}/convert`) → `Student`. A `Student.cohortId` is an explicit FK chosen at conversion/creation time — cohort membership is **not** derived from `startedAt`, so a cohort's reporting group is independent of when a student actually started training.
- **PipelineStage**: fixed `position` (ordering) and `normDays` (expected duration on that stage; `null` = no norm, e.g. the final stage). `Student.currentStageId` + `stageEnteredAt` drive health/overdue calculations.
- **Health/overdue** (`student.HealthCalculator`): `days = now - stageEnteredAt`; green if `days < 0.7 * normDays`, yellow `0.7–1.0x`, red `> 1.0x`; paused students are always `"paused"`; a stage with `normDays == null` is always green/never overdue.
- **Cohort funnel stats** (`statistics.StatisticsService.cohorts()`, backs the "Воронка доходимости по когортам" chart): for each non-archived cohort with ≥1 visible student, computes cumulative reached-count per pipeline stage (`currentStage.position >= stage.position`) plus a "planned stage position now" derived by walking `cohort.startDate` through cumulative `normDays`. The per-student on-track/behind split is computed from each student's own `startedAt`, not the cohort's `startDate` — students who joined late in the month aren't unfairly counted as behind.
- **Tests**: `AbstractIntegrationTest` boots a real Postgres 16 Testcontainer and runs actual Flyway migrations against it — integration tests exercise real schema + business rules, not mocks.

### Frontend

- React 19 + TypeScript + Vite, Tailwind v4 (via the PostCSS plugin). No state-management library — local hooks per page (`useState`/`useEffect`, small custom hooks).
- `src/api/client.ts`: single axios instance. Access token is kept **in memory only** (never localStorage). On a 401 (excluding the login/refresh routes themselves), it triggers a queued refresh: concurrent in-flight requests wait on the single refresh call instead of each firing their own; `registerAuthHooks` (wired by `AuthProvider`) supplies the actual refresh + auth-lost callbacks.
- Structure mirrors the backend domains: `src/pages/{leads,students,stats,admin}`, one `src/api/*.ts` per resource, `src/types` mirrors backend DTOs 1:1.
