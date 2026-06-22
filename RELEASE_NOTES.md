# Release notes

## v2.1.0 — sales→air→reconcile hardening

Two correctness fixes, a spot production lifecycle, a cart "freshness" engine,
show-targeting enforcement, and the first unit tests. One DB migration (auto-applied),
mostly **additive** API changes, and two **behavior changes** to know about.

### ⚠️ Heads-up (read first)

1. **Approval gate is enforced.** Only spots in `APPROVED`/`TRAFFICKED` air. The migration
   backfills existing spots to `APPROVED` (nothing currently running drops). New spots start
   `DRAFT` and must walk DRAFT → PRODUCED → APPROVED before they air.
2. **`SPECIFIC_SHOW` targeting is now honored.** `targetShowId` was previously stored but
   ignored; such spots now air **only** inside their target LibreTime show. Audit any
   stale/mis-set `targetShowId` values — those spots will stop airing elsewhere.
3. **Build requires JDK 21.** Lombok (Spring Boot 3.3.5) fails on JDK 25 with
   `ExceptionInInitializerError: ... TypeTag :: UNKNOWN`. Set `JAVA_HOME` to a JDK 21 if your
   default toolchain is newer. The Docker build is unaffected (pinned Temurin 21).

### DB migration — `v2-010` (Liquibase, auto-runs on boot, idempotent, forward-only)

- `spot.status` VARCHAR NOT NULL DEFAULT `'DRAFT'` — existing rows backfilled to `APPROVED`.
- `cart.selection_strategy` VARCHAR NOT NULL DEFAULT `'ROTATION'`.
- `cart.max_age_hours` INTEGER nullable.
- `cart_member.freshness_at` TIMESTAMP — existing rows backfilled to `created_at`.

### API changes (additive unless noted)

- **New:** `POST /api/spots/{id}/status` — body `{ "status": "DRAFT|PRODUCED|APPROVED" }`.
  `PRODUCED`/`APPROVED` require audio attached; `TRAFFICKED` is rejected (system-set on push).
- `SpotDto` responses now include `status`.
- `PUT /api/spots/{id}`: clearing the audio (`librtimeFileId=null`) resets status to `DRAFT`.
- `CartDto` now includes `selectionStrategy` and `maxAgeHours`.
- Cart `POST`/`PUT` accept optional `selectionStrategy` (`ROTATION`|`NEWEST_FIRST`) and
  `maxAgeHours` (on PUT: `-1` clears, `null` leaves unchanged).
- Spot create/update/delete auto-resync any `source=ORDER` cart for that order (side effect).

### Behavior internals

- **Reconciliation fix:** `push()` persists each row's computed air time (`scheduledAt`) plus
  resolved file/length, so reconciliation matches clock-built and cart/spot rows — not just
  preloaded ones. Push also flips `APPROVED`→`TRAFFICKED`.
- **Pagination fix:** the LibreTime client follows DRF `next` across all pages (library /
  show-instances / playout-history were truncated to page 1). Library browse/bulk-add now
  fetch the full file list — a server-side search is a future optimization for big libraries.
- **Cart resolver** gained: max-age filter, `NEWEST_FIRST` ordering (by `freshness_at`), the
  approval gate, and show targeting. Rotation/separation unchanged.

### Build / deploy / testing

- Version `2.0.0-SNAPSHOT` → `2.1.0` (pom.xml; surfaces via `/api/version`).
- Static assets cache-busted to `?v=20260622a` in `dashboard.html` — bump this whenever you
  change `app.js`/`app.css`.
- First tests under `src/test` (`TimeWindowUtilTest`, `CartServiceResolverTest`); `mvn test`
  runs them (13 tests).
- Deploy unchanged: `./deploy-docker.sh` (or `./ship.sh` to commit+push+deploy).

### Known constraints / not-done

- A clock applies **once per show instance** (matched on the instance start), not tiled per
  hour — multi-hour instances get one clock at the start; size the clock or split into hourly
  instances.
- Cart member `weight` is stored but unused (rotation is unweighted) — pre-existing.
- `PlaybackService.orderSummary` still uses `findAll()` — pre-existing scaling note.
