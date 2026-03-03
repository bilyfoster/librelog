# LibreLog QA Report

**Date:** 2026-03-03  
**Product:** LibreLog – GayPHX Radio Traffic System  
**Scope:** Backend (librelog-api), tests, docs, and deployment state  
**Updated:** Doc and path fixes applied (README, task markdown, coverage wording).

---

## Executive summary

- **Backend build and tests:** All Maven tests pass; `mvn clean test` and `mvn verify` succeed.
- **Deployed services:** LibreLog API and DB are running and healthy in Docker. Several dependent services (LibreTime playout/worker, legacy frontend/beat) are stopped.
- **Fixed:** README now uses `librelog-api` throughout; coverage section describes current JaCoCo policy; task/markdown files updated to `librelog-api` paths; fresh-start task note added. **Still to do:** Restart/debug stopped Docker services (LibreTime, librelog-frontend, librelog-beat); optionally add `frontend/README.md` (directory may be root-owned).

---

## What’s working

### 1. Backend (Java / Spring Boot)

- **Build:** `mvn clean compile` and `mvn clean package` succeed from repo root.
- **Tests:** Full suite passes (including Testcontainers/PostgreSQL).
  - Run: `mvn test` (from `/home/jenkins/docker/librelog`).
  - Run with coverage: `mvn clean test` then open `librelog-api/target/site/jacoco/index.html`.
- **Verify:** `mvn verify` passes (JaCoCo check runs and passes with current excludes).
- **Liquibase:** All 51 changelogs apply successfully during tests.
- **Deployed API:** Containers `librelog-api` and `librelog-db` are up and healthy (as of this run). API is behind Traefik at `log.gayphx.com`.

### 2. Project structure

- Multi-module Maven project: parent `librelog` and module `librelog-api` (no longer `librelog-web-api`).
- Static UI is served from the API: `librelog-api/src/main/resources/static/` (e.g. `index.html`, `dashboard*.html`, `libretime-integration-settings.html`, JS for clock-builder and libretime-integration).
- Tests are under `librelog-api/src/test/` (unit, integration, controllers, services, repos, enums, etc.).

### 3. Configuration

- `application.properties` is set up for DB, JWT, CORS, Liquibase, actuator, LibreTime, and static resources.
- Docker Compose for LibreLog (db + api) is valid; API uses Traefik labels for HTTPS and routing.

---

## What’s not working / needs attention

### 1. ~~README and docs use wrong module name~~ ✅ FIXED

- README and task markdown now use **`librelog-api`** for all Maven commands and paths. Correct commands: `mvn spring-boot:run -pl librelog-api`, `mvn liquibase:update -pl librelog-api`; coverage report at `librelog-api/target/site/jacoco/index.html`.

### 2. Code coverage below 80% for many packages

- **README** states: “Minimum 80% code coverage is required” and “Build will fail if coverage falls below 80%.”
- **Actual (JaCoCo report):**
  - **Overall:** ~37% instruction coverage, ~25% branch coverage.
  - **By package:** controllers ~20%, services.impl ~34%, config ~52%, security ~3%, utils ~67%, integrations ~85%, enums/repos (and specs) high or 100%.
- **Why `mvn verify` still passes:** The JaCoCo rule in `librelog-api/pom.xml` applies 80% limits only to a **subset** of code; large areas (controllers, services.impl, config, security, utils, integrations, dto, models, exceptions) are **excluded** from the check. So the build does not enforce 80% on the full codebase.
- **Impact:** Risk of regressions in low-coverage areas (especially controllers and security).
- **Recommendation:** Either:
  - Add tests and remove exclusions until overall coverage meets 80%, or
  - Update the README to describe the current policy (e.g. “80% required on non-excluded packages only”) and call out low-coverage areas.

### 3. Stopped Docker services (LibreTime and legacy LibreLog)

From `docker ps -a`:

- **libretime-liquidsoap**, **libretime-playout**, **libretime-worker:** Exited. LibreTime playout and worker are not running; features that depend on them (e.g. automated playout, some sync workflows) will not work until these are fixed and restarted.
- **librelog-frontend** (container): Exited. The current UI is served from the API’s `static/` folder; this container appears to be an older or alternate frontend. If you still need it, it needs debugging and a decision on how it fits with the current static UI.
- **librelog-beat** (Celery beat): Exited. Any scheduled/celery-based tasks for LibreLog are not running.

**Suggested next steps:**  
Inspect logs for the exited containers (`docker logs <container>`), fix config or dependencies, then restart. For LibreTime, follow LibreTime’s own docs for required services and order of startup.

### 4. ~~Task/markdown files reference old paths~~ ✅ FIXED

- `prd-fix-liquibase-test-failures.md` and `tasks-prd-python-to-java-backend-migration.md` now use `librelog-api` paths. `tasks-fresh-start-clean-spring-boot-foundation.md` has a note that the current module is **librelog-api**.

### 5. Root `frontend/` directory

- **Observation:** `frontend/` has no `package.json`; the active UI is in `librelog-api/src/main/resources/static/`.
- **Impact:** Unclear whether this directory is legacy or planned for a future frontend. If unused, consider removing or documenting as “reserved for future SPA” to avoid confusion.

---

## Test and build commands (quick reference)

From repo root: `/home/jenkins/docker/librelog`

```bash
# Build
mvn clean compile

# Run all tests (Docker required for Testcontainers)
mvn test

# Run tests and enforce coverage rules
mvn verify

# Run API locally (DB must be reachable, e.g. local or Docker)
mvn spring-boot:run -pl librelog-api

# Package JAR
mvn clean package -DskipTests
```

---

## Suggested order of work

1. ~~**Immediate:** Update README and docs to use `librelog-api` and fix coverage wording.~~ ✅ Done.
2. ~~**Short term:** Document current coverage policy in README.~~ ✅ Done.
3. **Operational:** Debug and restart LibreTime containers (liquidsoap, playout, worker) and, if still needed, `librelog-frontend` and `librelog-beat`.
4. ~~**Cleanup:** Align task/markdown files with current architecture.~~ ✅ Done. Optionally add `frontend/README.md` (see §5 above).

---

## Environment used for this QA run

- **Java:** OpenJDK 21.0.10  
- **Maven:** 3.8.7  
- **Project:** LibreLog 0.0.13-SNAPSHOT, module `librelog-api`  
- **Docker:** librelog and Traefik networks present; librelog-api and librelog-db containers running.
