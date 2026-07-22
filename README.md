# LibreLog v2

Traffic + scheduling for LibreTime. This is the v2 MVP rewrite. The previous codebase
lives on the [`archive/v1`](https://github.com/bilyfoster/librelog/tree/archive/v1)
branch (tag [`v1-final`](https://github.com/bilyfoster/librelog/releases/tag/v1-final))
and is read-only.

## What v2 does

- Connect a station to LibreTime (URL + API key, with a Test button).
- Browse the LibreTime library, show instances, smart blocks, and playlists (read-only).
- Manage customers, orders, and spots, with a production lifecycle per spot
  (DRAFT → PRODUCED → APPROVED → TRAFFICKED). Only approved spots air; a spot becomes
  TRAFFICKED automatically when it is pushed into a schedule. Editing an order's spots
  re-syncs any order-backed commercial cart automatically.
- Carts resolve at push time with a selectable strategy: ROTATION (round-robin, for music
  and commercials) or NEWEST_FIRST with an optional max-age window (for news and
  voicetracks, so stale audio never airs). Spots flagged for a specific show only air
  inside that show.
- Build a day's schedule against existing LibreTime show templates, save as a draft, and
  push it to LibreTime. Push is blocked for today/past days — logs go out at least a day
  ahead so active playout buffers are never rewritten.
- Music burnout rules per the Rumble PRD: 90-minute artist / 240-minute song separation
  floors on music carts (per-cart policies can only be stricter), plus clutter control so
  the same sponsor never lands in adjacent slots when an alternative exists.
- Upload audio (spots, promos, voice tracks) from the dashboard: ffmpeg loudnorm
  (-14 LUFS / -2.0 dBFS TP) to MP3 44.1kHz 192k CBR stereo, then SFTP to the Jazz
  (LibreTime) drop zone with an automatic library import trigger.
- Browser voice tracking: VOICETRACK clock slots show as empty VT slots in the day
  builder; record a take in the browser (with segue/duck markers) and it is transcoded,
  handed to Jazz, and attached to the slot.
- Per-day editor lock with a 15-minute TTL so two users can't clobber each other.
- Pull as-run playback history from LibreTime and reconcile by order. A nightly job
  (03:30, configurable) imports yesterday's log per station and the dashboard shows
  per-order fulfillment (played X of Y) with make-up spots owed.

For the full plan and rationale see the project plan on the `mvp` branch.

## Running locally

Requirements: JDK 21, Maven 3.9+, Docker (with Compose), Postgres 16 (or use the
docker-compose).

```bash
# build
mvn -DskipTests clean package

# start with docker-compose (also runs Postgres)
docker compose up -d --build

# tail logs
docker compose logs -f api
```

The dashboard is at <http://localhost:8080> (or <https://log.gayphx.com> when deployed
behind Traefik).

The first run seeds an admin user from `ADMIN_SEED_EMAIL` / `ADMIN_SEED_PASSWORD`
(defaults `admin@librelog.local` / `admin123`). Change those env vars and the password
on first login.

## Configuration

| Env var | Default | Notes |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/librelog` | |
| `SPRING_DATASOURCE_USERNAME` | `librelog` | |
| `SPRING_DATASOURCE_PASSWORD` | `librelog` | |
| `JWT_SECRET` | `change-me-...` | Min 32 bytes |
| `JWT_EXPIRATION_MINUTES` | `480` | |
| `ADMIN_SEED_EMAIL` | `admin@librelog.local` | Only used on first boot |
| `ADMIN_SEED_PASSWORD` | `admin123` | Change immediately |
| `ENCRYPTION_KEY` | `change-me-...` | Used to AES-GCM encrypt LibreTime API keys at rest |
| `SERVER_PORT` | `8080` | |
| `JAZZ_SFTP_HOST` | _(empty)_ | Jazz/LibreTime host for audio handoff; empty = local-only uploads |
| `JAZZ_SFTP_USER` | `rumble_bridge` | |
| `JAZZ_SFTP_PORT` | `22` | |
| `JAZZ_SFTP_KEY_PATH` | _(empty)_ | Private key mounted into the container — never bake it into the image |
| `JAZZ_IMPORT_PATH` | `/srv/libretime/rumble_import` | Drop zone on Jazz |
| `JAZZ_IMPORT_TRIGGER` | `/usr/local/bin/rumble_import_trigger.sh` | Runs on Jazz after upload |
| `LIBRELOG_RECONCILIATION_CRON` | `0 30 3 * * *` | Nightly as-run import + make-up summary |

## Layout

```
pom.xml                              single Maven module
Dockerfile                           builds the JAR image
docker-compose.yml                   api + db + Traefik labels
deploy-docker.sh                     mvn package -> compose up
src/main/java/com/onelpro/librelog/
  config/        security, encryption, app properties
  auth/          AppUser, JWT, login, users CRUD, admin seeder
  station/       Station CRUD
  librtime/      LibreTime client, connection, browse endpoints
  customers/     Customer CRUD
  orders/        Order + Spot CRUDs
  schedule/      Day Builder, schedule items, day locking, push
  playback/      Playback log import, reconciliation, nightly job, fulfillment
  rumble/        Rumble PRD services: audio transcode, Jazz SFTP handoff,
                 media uploads, voice tracks, rotation constants
                 (the rumble/domain entities are unused PRD scaffolding — the
                 live model is the v2 schema above)
src/main/resources/
  application.yml
  db/changelog/  Liquibase initial schema
  static/        dashboard.html, app.js, app.css
```

## Deploying to log.gayphx.com

```bash
./deploy-docker.sh
```

This builds the jar, rebuilds the api image, restarts the stack, and waits for
`https://log.gayphx.com/actuator/health` to come up.

> Note: the Dockerfile healthcheck talks to `http://127.0.0.1:8080` (IPv4) on purpose.
> `localhost` resolves to `::1` inside Alpine but Spring Boot binds 0.0.0.0, so an
> IPv6-targeted healthcheck silently times out.
