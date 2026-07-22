# Release notes

## v2.2.1 — editable cart categories + INTERVIEW category

- **Cart category is now editable** after creation (cart → Edit). The category is
  validated against the cart's type: library carts (music/content) accept library
  categories, spot carts accept commercial categories. Members and separation policy
  are untouched. Cart *kind* (library vs. spot) remains immutable — members are
  validated against it.
- **New library category `INTERVIEW`** (240-min same-title default separation) so
  interviews can rotate separately from generic CONTENT.
- No DB migration. Version 2.2.0 → 2.2.1.

## v2.2.0 — Rumble: safety rails, audio handoff, burnout rules, voice tracking, make-ups

First Rumble (PRD) release: the playout safety rails, the audio processing + SFTP
handoff to Jazz, PRD burnout rules in the resolver, browser voice tracking, and the
nightly as-run/make-up loop. Two DB migrations (auto-applied), additive APIs, and a few
**behavior changes** to know about.

### ⚠️ Heads-up (read first)

1. **Push is now blocked for today and past days.** The day builder refuses to push a log
   whose date is before tomorrow (PRD §7 lock window — active playout buffers are never
   rewritten). Build and push logs at least a day ahead. Admin `reopen` is unchanged.
2. **Music separation floors are now 90 min artist / 240 min song** (PRD SCH-03) for
   MUSIC-category carts. Per-cart policies can only be stricter, never looser. New music
   carts default to 90/240 (previously 15/180). Other categories (NEWS, etc.) are
   unaffected. With small carts this can leave slots unresolvable — the resolver falls
   back to *some* eligible member and flags the violation in preview/push notes rather
   than leaving dead air.
3. **Clutter control:** the fallback path no longer places the same sponsor in two
   adjacent slots when any other eligible member exists; if unavoidable it is flagged.
4. **Docker image now ships ffmpeg** (audio pipeline); the compose stack adds a
   `transcode_data` volume and `JAZZ_*` env vars. Uploads/voice tracks stay local
   (status `UPLOADED`) until `JAZZ_SFTP_*` is configured — see
   `docs/rumble-production-ops.md`.

### DB migrations — `v2-012`, `v2-013` (Liquibase, auto-run on boot)

- `v2-012`: `media_upload` table (audio upload tracking: names, tags, duration,
  status UPLOADED/IMPORTED/FAILED, error).
- `v2-013`: `schedule_item.segue_offset_seconds` INT, `schedule_item.duck_db` DECIMAL(4,1)
  — voice-track overlap markers.

### What's new

- **Audio Uploads panel** (`POST/GET /api/media/uploads`): upload a spot/promo/VT audio
  file → ffmpeg loudnorm (I=-14, TP=-2.0) → MP3 44.1kHz 192k CBR stereo with ID3 tags →
  SFTP to the Jazz drop zone → remote `rumble_import_trigger.sh` import. Per-row status
  and error in the dashboard.
- **Voice tracking:** clock slots of kind `VOICETRACK` render as "Empty VT Slot" in the
  day builder with a Record button. The browser recorder (MediaRecorder) captures a take,
  transcodes it (title `VT-[Host]-[Date]-[Hour]`, PRD AUD-04), hands it to Jazz, and
  attaches the imported file id to the slot. Segue offset / duck markers save with the
  take; the segue maps to LibreTime `fade_out` at push (duck is stored in LibreLog only —
  LibreTime has no schedule field for it).
- **Preview violations:** `GET /api/days/{id}/preview` rows now carry a `violation` flag
  (separation/clutter), rendered in red in the preview modal.
- **Nightly reconciliation job** (`librelog.reconciliation.cron`, default 03:30): imports
  yesterday's playout history per connected station and logs ordered-vs-played with
  make-up counts. New `GET /api/playback/fulfillment?stationId&date` powers a
  "Fulfillment / make-ups" panel listing played X of Y and the owed spot labels per order.

### Fixes

- `PlaybackService.orderSummary` no longer scans `findAll()` — scoped to
  `findBySpotIdIn` (was on the nightly path).

### Notes for the next dev

- The PRD's `rumble_client` / `rumble_campaign` / `rumble_media_asset` / `rumble_format_clock*`
  / `rumble_schedule_grid` tables (v2-011) remain **unused scaffolding** — the live model
  is the v2 customers/orders/spots/carts/clocks schema. Build on v2; revisit the rumble
  tables only if the PRD model is revived.

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
