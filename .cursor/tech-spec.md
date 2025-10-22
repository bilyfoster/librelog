

# LibreLog Technical Specification
### Project: GayPHX Radio Automation & Traffic System
### Version: 1.0
### Author: GayPHX Engineering Team

---

## 📘 Overview
LibreLog is a modular radio automation, scheduling, and traffic management system designed for **GayPHX Radio**, powered by **LibreTime** and **AzuraCast**. It acts as a smart middleware layer — handling metadata tagging, daily log generation, voice tracking, and integration between automation and streaming systems.

---

## 🧱 System Architecture
LibreLog is built around a **three-layer architecture**:

1. **Frontend (Web UI)**
   - Built using **React (Next.js)** or **Vite + React**.
   - Provides views for library management, ad campaign scheduling, clock builder, and reporting.
   - Integrates with backend APIs via REST and WebSockets.

2. **Backend (API & Services)**
   - Built in **Python (FastAPI)** for REST and async scheduling services.
   - Uses **PostgreSQL** as the database.
   - Includes modular microservices:
     - `log_generator` – builds and publishes daily logs.
     - `libretime_exporter` – syncs scheduled logs to LibreTime.
     - `ad_fallback` – ensures PSA fills in if ads unavailable.
     - `azuracast_sync` – syncs now-playing metadata to AzuraCast.
     - `libretalk_socket` – handles live voice-tracking or breaks.

3. **Integration Layer**
   - Communicates with:
     - **LibreTime API** (`/api`) – for scheduling & playout.
     - **AzuraCast API** – for metadata and listener stats.
     - **LibreTalk** (future) – for voice tracking and automation break handling.

---

## ⚙️ Core Components

### 1. Media Library
- Organizes tracks by `type`: MUS, ADV, PSA, LIN, INT, PRO, BED.
- Supports tagging and duration analysis.
- Scheduled auto-clean and re-sync job nightly.

### 2. Clock Templates
- Define hour layouts using Smart Block logic.
- JSON structure example:
  ```json
  {
    "hour": "06:00",
    "elements": [
      {"type": "LIN", "title": "Top-of-Hour ID"},
      {"type": "BED", "title": "News Bed"},
      {"type": "INT", "title": "Morning Interview"},
      {"type": "ADV", "fallback": "PSA"},
      {"type": "MUS", "count": 3}
    ]
  }
  ```

### 3. Log Builder
- Pulls daily templates and generates 24-hour logs.
- Includes ad insertion, PSAs, and voice tracks.
- Publishes final log JSON to LibreTime.

### 4. Voice Tracking
- Upload or record short breaks between segments.
- Automatically associates tracks with scheduled blocks.
- Future integration: LibreTalk live socket events.

### 5. Traffic Management
- Campaign model defines start/end date, advertiser, and priority.
- Ad logic:
  - High-priority ads preempt lower ones.
  - If no active ad, insert PSA from same category.

### 6. Reporting & Compliance
- Generates playback reconciliation reports.
- Exports CSV/PDF for SOCAN, ASCAP, or FCC-style compliance.

---

## 🧩 APIs and Endpoints
See `.cursor/api-schema.yaml` for OpenAPI 3.0 schema.

---

## 🗄 Database Layer
- **PostgreSQL** backend.
- Schemas managed via Alembic.
- Tables: `tracks`, `campaigns`, `daily_logs`, `clock_templates`, `playback_history`, etc.
- View: `reconciliation_report`.

---

## 🔐 Security & Authentication
- All API calls require JWT tokens.
- OAuth2 support for trusted apps (e.g., LibreTime backend).
- Admin roles required for campaign and log publication.

---

## 🧠 Smart Logic Rules
- **ADV → PSA Fallback:** If an ad is not available or expired, automatically replace it with a public service announcement.
- **Daypart Filters:** Smart blocks can restrict genres/time of day (e.g., “morning soft pop” vs. “late-night club beats”).
- **Voice Priority:** If a voice track exists for a slot, it overrides other content.

---

## 📡 Integrations Summary
| Integration | Purpose | Auth Type | Status |
|--------------|----------|------------|---------|
| LibreTime | Schedule, playout logs | API Key | ✅ |
| AzuraCast | Stream metadata & listener stats | API Key | ✅ |
| LibreTalk | Voice tracking socket | Future | 🔄 |

---

## 🧰 Tools & Libraries
- **Backend:** FastAPI, SQLAlchemy, Celery, Alembic
- **Frontend:** React, MUI, Axios, Chart.js
- **Infrastructure:** Docker Compose, Nginx, Redis, PostgreSQL
- **Testing:** PyTest, Jest

---

## 🚀 Deployment
- Docker-based multi-service setup.
- `docker-compose up` will start:
  - `api` (FastAPI)
  - `frontend`
  - `db`
  - `worker`
  - `nginx`
- Production deployments managed via GitHub Actions and Watchtower.

---

## 📅 Roadmap
| Milestone | Target Date | Description |
|------------|--------------|--------------|
| Alpha | Q4 2025 | Basic scheduling, logs, and API sync |
| Beta | Q1 2026 | Clock templates, campaigns, voice tracking |
| Full Launch | Q2 2026 | Public station integration + reports |

---

## ✨ Vision
LibreLog aims to make **community radio feel professional**, giving small queer-owned and independent stations the same powerful tools commercial broadcasters use — with automation that still sounds human.