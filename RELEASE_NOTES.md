# Release notes

## v2.8.0 — multi-part feature packages (interviews as break points, no slicing)

Long-form interviews air in parts around breaks — from **one uploaded file with break
points** (parts play as `cue_in`/`cue_out` windows into the same file) or from
separately edited part files. One DB migration (auto-applied), additive APIs.

### What's new

- **Feature packages** (Audio Uploads → Feature packages): create a package, add parts
  either as *one file + break points* ("12:00, 23:00" → three cue windows) or as
  *separate part files*. DRAFT → **READY** (assignable) → AIRED (set by push).
- **FEATURE clock slots**: "Feature part (interview segment)" in the slot picker with a
  part number. Build a Specialty Interview Hour once: teaser, part 1, break, part 2,
  break, part 3, music, pad.
- **Per-day assignment**: each show block in Day Builder gets a 🎙 button — assign a
  READY package to that show; unassigned FEATURE slots are flagged at preview/push and
  skipped with a clear note. Push flips the package to AIRED.
- **Back-timing absorbs part-length variance** (Phase A): if part 2 runs 11:23 instead
  of 11:00, the end-of-hour music/pad shrinks automatically — the top of the hour still
  lands exactly.
- Features are **never trimmed** (the never-cut rule).

### Schema / API

- Migration `v2-017`: `media_package`, `media_package_part` (cue windows), 
  `feature_assignment`, `feature_sequence` on clock slots + schedule items.
- New endpoints: `GET/POST /api/stations/{id}/packages`, `PUT/DELETE /api/packages/{id}`,
  `PUT /api/packages/{id}/parts`, `PUT/DELETE /api/days/{dayId}/instances/{iid}/feature`.
  `DayDto` gains `featureAssignments`.
- `scheduleFileInInstance` now supports non-zero `cue_in` (validate on the first real
  push that the Jazz build honors API-set cue_in — the multi-file mode is the fallback).

### Tests

- Planner: feature parts as cue windows of one file + pad-to-end (77 tests total).

## v2.7.0 — hot-clock timing engine: anchors, back-timing, avail caps

Push is now **plan-then-write** per show instance, run by a new segment planner
(`ClockSegmentPlanner`). One DB migration (auto-applied), additive APIs, and two
behavior refinements to know about.

### ⚠️ Heads-up

1. **Every hour is now back-timed.** The last music/pad unit before a hard boundary
   (the top of the hour, or a HARD anchor) is trimmed via LibreTime `cue_out` (floor
   20s) so the boundary lands exactly. Non-music content (spots, interviews) is
   **never cut** — overruns are flagged in push notes instead. `TO_END` fills now end
   exactly at the instance end (previously the last song could run past it).
2. **Preview no longer nudges rotation.** Preview used to persist rotation-pointer
   advances; it now runs a dry-run resolver (realistic sequence, nothing saved).

### What's new

- **Anchors**: any clock slot can be pinned `@mm:ss` from the show start (legal ID at
  0:00, break A at 18:00), SOFT (start late + flag) or HARD (trim preceding music to
  land exactly). Editor shows an ⚓ and flags floating content that overshoots an anchor.
  Anchors must be strictly increasing; validated server-side.
- **Pad/sweeper source**: per-station pad cart (Station form; fallback = first IMAGING
  cart) fills underruns before anchors and small gaps at the end of the hour
  (≤90s; bigger underruns are flagged as clock problems, not padded).
- **Avail caps**: a COUNT fill can also carry a total-seconds cap ("max 3 spots /
  120s"); expanded units share a `fill_group` and the cap is enforced across the
  group at push (15s grace).
- **Preview shows the plan**: planned air times (station-local), trims, pads, anchor
  misses and timing notes — the same math push will apply. Response shape is now
  `{items, notes}` with `plannedAt`/`lengthSeconds` per row.

### Schema / API

- Migration `v2-016`: `anchor_offset_seconds` + `anchor_policy` on
  `clock_template_slot` and `schedule_item`; `fill_group` on `schedule_item`;
  `pad_cart_id` on `station`.
- Slot/item DTOs and station DTO carry the new fields (additive).

### Tests

- `ClockSegmentPlannerTest` (9 tests): hard-anchor trim, soft-late flag, pad-to-anchor,
  tiny-gap early start, avail seconds cap, TO_END exact top-of-hour, never-trim rule,
  dead-air shift. 76 tests total.

## v2.6.0 — category-first clock slot picker

Frontend-only redesign of the clock slot editor. No DB or API changes — the same
kind/cartId/cartCategory payload is produced underneath.

- The slot row's first dropdown is now **"what plays here"**, listing the station's
  actual editorial categories (Music, IDs & imaging, News, Interviews, Promo,
  Commercial break, Sponsored feature, …) sourced from `/api/cart-categories`, plus
  **Specific cart** (carts grouped by category), **Fixed track**, **Specific spot**,
  **Voice track**, **Ad slot placeholder**, and **Note**.
- Picking a category maps to the category-pool binding (least-recently-aired cart of
  that category resolves at push); picking a specific cart derives the underlying kind
  from the cart itself — users no longer need to know "music cart" means "library cart".
- Runtime estimates now use the category/cart to choose the 180s-vs-30s default unit.
- Clocks-tab help text rewritten to match.

## v2.5.1 — deploy script honesty

`deploy-docker.sh` previously "succeeded" by curling the public URL's health endpoint —
which passes even when you deploy from a machine that does **not** serve log.gayphx.com
(the site kept running an old version while the script said OK). It now:

- verifies the **local** container came up on the version this checkout built
  (`/api/version` inside the container must match pom.xml), and
- compares the **public** site's version: if it differs, it says plainly that the public
  site is hosted elsewhere and this deploy only updated the local stack (exit 1).

No app changes; version bumped so the fix is traceable.

## v2.5.0 — clock editor: timing feedback + reorder

Frontend-only quality pass on the clock slot editor. No DB or API changes.

- **Estimated start offset per row** (`@3:30`) computed from default lengths and fill
  targets, live as you type. TO_END rows show `→end`.
- **Est. runtime total** next to Save slots, with a red warning when the clock sums past
  60:00 (push would drop the overflow).
- **Reorder** slots with ↑/↓ buttons (save order = display order, as before).
- Fixed: the remove (x) button on rows added after opening the editor was dead (handler
  was bound only at render); remove/reorder are now delegated.

## v2.4.0 — weekly clock assignment grid

The standing format lives at the station now: a **weekly grid** (weekday × station-local
window → clock) that every new schedule day seeds from automatically. Per-day clock
schedule rows are unchanged and remain the override mechanism. One DB migration,
additive APIs, no behavior changes for existing days.

- **Clocks tab → "Weekly clock grid"**: rows of (weekday, start–end local time, clock).
  Saved per station (`GET/PUT /api/stations/{id}/clock-grid`).
- **Auto-seed**: the first time a day is opened in Day Builder (i.e. its `schedule_day`
  row is created), its clock schedule is copied from the grid rows for that weekday.
  Existing days are never re-seeded.
- **Day Builder → "Grid defaults" button**: replaces the current day's clock-schedule
  rows with the grid's weekday rows (needs the day lock; blocked on pushed days). Use it
  for days created before the grid existed, or to reset after experimenting.
- Migration `v2-015`: new `station_clock_grid` table. Validation mirrors the per-day
  editor (window pairs, clock-belongs-to-station, ISO weekday 1–7).
- Tests: grid seeding on day creation + no-reseed guarantee (67 total).

## v2.3.0 — fill blocks + fair advertiser rotation

Clock hours can now contain *blocks*, not just single units, and category pools rotate
advertisers fairly. One DB migration (auto-applied), additive APIs, one behavior change.

### ⚠️ Heads-up

1. **Category-pool cart order changed (fairness).** Slots bound to a *category* (e.g.
   "any COMMERCIAL cart") now try the cart that has aired **least recently** first,
   instead of alphabetical name order. Expect advertiser rotation to even out — carts
   that used to win every break because they sorted first will now share.
2. Fill semantics: **COUNT/TIME blocks expand into concrete unit slots when the clock is
   applied** (each unit resolves independently at push, with real schedule items for
   order reconciliation). **TO_END is music-only**, must be the clock's last slot, and
   resolves unit-by-unit at push straight into LibreTime (play history recorded; no
   per-unit schedule items — filler music has no order to reconcile).

### What's new

- **Fill blocks on cart slots** (`fillMode` on clock slots):
  - `COUNT` — a fixed number of units (1–50), e.g. a 4-unit ad break from the
    COMMERCIAL category pool.
  - `TIME` — fill ~N seconds (30–3600), unit-estimated from the slot's default length
    (30s commercial / 180s music), e.g. a 3:00 stopset = 6 × 30s units.
  - `TO_END` — music only: keep resolving songs until the show instance ends ("take us
    to the top of the hour"). Safety-capped at 60 units; the marker item records the
    fill's start time and total seconds after push, and push notes report what filled.
- **Fair pool rotation**: `Resolver.orderPoolFairly` orders category-pool carts
  least-recently-aired first (never-aired first, ties by name), using persisted history
  *plus* picks already made in the current push — so consecutive pool slots rotate
  across clients within one break.
- Clock editor: fill controls per cart slot row (single / fill # units / fill seconds /
  to end).

### API / schema

- Migration `v2-014`: `fill_mode`, `fill_target_seconds`, `fill_target_count` on
  `clock_template_slot` and `schedule_item`.
- Clock slot DTOs and schedule item DTOs carry the three fill fields (additive).
- Validation: fill modes only on cart slots; TO_END only on music slots and only as the
  last slot of a clock.

### Tests

- `ScheduleServiceFillTest` (COUNT/TIME/TO_END expansion math) and two new resolver
  tests for fair pool ordering (65 tests total).

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
