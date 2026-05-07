# LibreLog v1 — Archived

This branch (`archive/v1`) preserves the v0.1.9 state of LibreLog at tag `v1-final`. It is
**read-only / reference only**. Active development has moved to the `mvp` branch
(LibreLog v2). This document captures what was built, what worked, what didn't, and why
we chose to start fresh.

## Purpose of v1

LibreLog v1 attempted to be a multi-station, multi-tenant traffic and scheduling system
that integrated with [LibreTime](https://libretime.org) to:

- Manage organizations, markets, clusters, channels, and stations.
- Manage advertisers, agencies, sales reps, orders, and campaigns.
- Build clock templates, dayparts, and daypart categories.
- Auto-generate hour-by-hour logs honoring separation rules.
- Push generated logs to LibreTime and ingest playback history back.

## Snapshot of features at v0.1.9

Pulled from [VERSIONS.md](VERSIONS.md):

- **0.1.4** — Base MVP with campaign management, station/clock/daypart infrastructure,
  LibreTime integration foundation.
- **0.1.5** — Auth login field compatibility (`username` or `email`), `PUT /auth/profile`
  500 fix.
- **0.1.6** — JWT filter authentication for `/api/auth/me` and `/api/auth/profile`.
- **0.1.7** — `advertiser_id` foreign key on orders; `Order` ↔ `Advertiser` relationship.
- **0.1.8** — Voice track song context (`song_before_id`, `song_after_id`).
- **0.1.9** — `POST /api/campaigns/from-order/{orderId}` workflow; declared "MVP complete".
- **post-0.1.9 (unversioned)** — Range schedule generation, scheduling exception reports,
  scheduler-first dashboard simplification, IPv4 healthcheck fix in
  [`Dockerfile`](Dockerfile).

## What worked (carry forward as reference, not as code)

- LibreTime HTTP wire format and auth header shape in
  [`integrations/LibreTimeClient`](librelog-api/src/main/java/com/onelpro/librelog/integrations).
- `LibreTimeExportDTO` shape — what LibreTime accepts on push.
- JWT auth, password hashing.
- Liquibase migrations (changelog ordering as a rough schema sketch).
- Docker + Traefik deploy at `log.gayphx.com`. The IPv4 (`127.0.0.1`) healthcheck fix in
  [`Dockerfile`](Dockerfile) is a non-obvious gotcha worth carrying forward.

## What didn't work (drives v2 scope)

1. **Scope sprawl before users.** Organizations, markets, clusters, channels, agencies,
   sales reps, daypart categories, fixed assets, automation commands, grids, daypart
   assignments — none had a real user demanding them. They added schema, screens, tests,
   and migration friction.
2. **Auto-scheduler reliability.** The hour-by-hour generator
   ([`LibreTimeSyncServiceImpl.generateLogInternal`](librelog-api/src/main/java/com/onelpro/librelog/services/impl/LibreTimeSyncServiceImpl.java))
   kept failing because there was no human-in-the-loop draft step and the rules
   (separation, daypart, artist cooldowns) couldn't always be satisfied.
3. **Dashboard complexity.** ~15 nav tabs, no clear primary workflow. Scheduling — the
   actual product — was buried.
4. **Build/test friction.** Multi-module Maven layout
   ([parent pom](pom.xml) + [api pom](librelog-api/pom.xml)), JaCoCo 80% coverage gates,
   and dependencies that weren't earning their keep yet (bucket4j rate limiting, POI/CSV
   bulk import, springdoc swagger, wiremock, testcontainers) slowed every iteration.
5. **Hourly as the unit of work.** Users care about days. Hours were a leaky abstraction
   for both the scheduler and the UI.

## Pointer to v2

Active development continues on the `mvp` branch:

```bash
git checkout mvp
```

LibreLog v2 is a deliberate restart with the same Spring Boot + Postgres stack, slimmed
to a single Maven module and one workflow: connect a station to LibreTime, build a day's
schedule from existing LibreTime show templates, push it back, and reconcile against the
playback log.

See the v2 plan for full scope and phasing.
