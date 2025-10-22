.cursor/project-plan.json
{
  "project": "LibreLog – GayPHX Radio Traffic System",
  "epics": [
    {
      "id": "EPIC-001",
      "title": "Core Scheduling Engine",
      "tasks": [
        {
          "id": "TASK-001",
          "title": "Integrate LibreTime API",
          "description": "Connect to LibreTime v2 API to fetch Smart Blocks and scheduling data.",
          "status": "todo"
        },
        {
          "id": "TASK-002",
          "title": "Build Log Generator",
          "description": "Create hourly logs based on scheduling data and clock templates.",
          "status": "todo"
        }
      ]
    },
    {
      "id": "EPIC-002",
      "title": "Campaign & PSA Management",
      "tasks": [
        {
          "id": "TASK-003",
          "title": "Design Campaign Data Model",
          "description": "Define database schema for campaigns, ads, and PSAs.",
          "status": "todo"
        },
        {
          "id": "TASK-004",
          "title": "Implement Campaign Scheduling",
          "description": "Schedule campaigns and PSAs into logs respecting priority and timing.",
          "status": "todo"
        }
      ]
    },
    {
      "id": "EPIC-003",
      "title": "AzuraCast Integration",
      "tasks": [
        {
          "id": "TASK-005",
          "title": "Send Now-Playing Metadata",
          "description": "Push current playing track info to AzuraCast API.",
          "status": "todo"
        }
      ]
    },
    {
      "id": "EPIC-004",
      "title": "User Interface",
      "tasks": [
        {
          "id": "TASK-006",
          "title": "Build React Frontend",
          "description": "Create UI for managing Smart Blocks, campaigns, and logs.",
          "status": "todo"
        }
      ]
    }
  ]
}

.cursor/tasks.yaml
- id: TASK-001
  title: Integrate LibreTime API
  description: Connect to LibreTime v2 API to fetch Smart Blocks and scheduling data.
  status: todo
  file: src/libretime_integration.py
- id: TASK-002
  title: Build Log Generator
  description: Create hourly logs based on scheduling data and clock templates.
  status: todo
  file: src/log_generator.py
- id: TASK-003
  title: Design Campaign Data Model
  description: Define database schema for campaigns, ads, and PSAs.
  status: todo
  file: db/schema.sql
- id: TASK-004
  title: Implement Campaign Scheduling
  description: Schedule campaigns and PSAs into logs respecting priority and timing.
  status: todo
  file: src/campaign_scheduler.py
- id: TASK-005
  title: Send Now-Playing Metadata
  description: Push current playing track info to AzuraCast API.
  status: todo
  file: src/azuracast_integration.py
- id: TASK-006
  title: Build React Frontend
  description: Create UI for managing Smart Blocks, campaigns, and logs.
  status: todo
  file: frontend/src/App.tsx

.cursor/tech-spec.md
# LibreLog Technical Specification

LibreLog is a GayPHX-built broadcast traffic and automation manager for LibreTime and AzuraCast.

## Goals
- Build professional-grade automation for GayPHX Radio.
- Manage Smart Blocks, hourly clocks, PSAs, ads, and interviews.
- Integrate LibreTime for scheduling and AzuraCast for streaming metadata.

## Stack
- Backend: FastAPI (Python 3.11)
- Frontend: React + TypeScript
- DB: PostgreSQL
- APIs: LibreTime v2, AzuraCast v1

.cursor/api-schema.yaml
openapi: 3.0.0
info:
  title: LibreLog API
  version: 1.0.0
paths:
  /tracks:
    get:
      summary: List all tracks
  /logs/generate:
    post:
      summary: Generate a daily log

.cursor/db-schema.sql
CREATE TABLE track_metadata (
  id SERIAL PRIMARY KEY,
  libretime_id UUID,
  rotation_category VARCHAR(50),
  daypart VARCHAR(20),
  mood VARCHAR(30),
  tempo VARCHAR(10),
  last_sync TIMESTAMP DEFAULT NOW()
);

CREATE TABLE campaigns (
  id SERIAL PRIMARY KEY,
  advertiser VARCHAR(100),
  start_date DATE,
  end_date DATE,
  priority INT,
  file_url TEXT
);

.cursor/architecture-diagram.md
# Architecture Overview

LibreTime → LibreLog → AzuraCast

1. LibreTime provides Smart Blocks and media via API.
2. LibreLog builds hourly logs and sends them back.
3. AzuraCast receives now-playing metadata.

.cursor/env.template
LIBRETIME_URL=https://studio.gayphx.com/api
LIBRETIME_KEY=changeme
AZURACAST_URL=https://radio.gayphx.com/api
AZURACAST_KEY=changeme
POSTGRES_URI=postgresql://librelog:password@db/librelog

.cursor/prompt-context.md
# Prompt Context: LibreLog

LibreLog is a GayPHX-built tool that turns LibreTime + AzuraCast into a full radio traffic system.
Core concepts: Smart Blocks, Clock Templates, Campaigns, Voice Tracking.
Morning interviews run 6–10 AM, other hours rotate music, PSAs, and promos.