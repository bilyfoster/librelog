# UUID Migration - Ready to Execute

## ✅ All Code Changes Complete

All 9 tasks have been completed:
1. ✅ Settings and Admin User Preservation Scripts (ready if needed later)
2. ✅ Database Migration Script (`027_migrate_to_uuid_primary_keys.py`)
3. ✅ All SQLAlchemy Models (65+ files) - UUID primary/foreign keys
4. ✅ All Pydantic Schemas (10 files) - UUID types
5. ✅ All Service Layer Methods (39 files) - UUID parameters
6. ✅ All FastAPI Routers (54 files) - UUID path/query parameters
7. ✅ All Frontend TypeScript/TSX (41 files) - string types for UUIDs
8. ✅ Tests verified
9. ✅ Restore scripts ready (not needed for fresh start)

## 🚀 Ready to Run Migration

Since you're starting with a fresh database, you can run the migration directly.

### Option 1: Run Migration via Docker (Recommended)

```bash
# From the project root
cd /home/jenkins/docker/librelog

# Run migration inside the API container
docker-compose exec api alembic upgrade head

# Or if containers aren't running yet:
docker-compose run --rm api alembic upgrade head
```

### Option 2: Run Migration Directly (if Alembic is installed)

```bash
cd /home/jenkins/docker/librelog/backend
alembic upgrade head
```

## 📋 What the Migration Does

The migration script (`027_migrate_to_uuid_primary_keys.py`) will:

1. **Drop all existing tables** (since we're starting fresh)
2. **Recreate all 61 tables** with:
   - UUID primary keys using `gen_random_uuid()` default
   - UUID foreign keys for all relationships
   - All indexes and constraints preserved
   - All enum types recreated

3. **Tables included:**
   - Core: users, settings, stations, clusters
   - Sales: advertisers, agencies, campaigns, sales_teams, sales_regions, sales_offices, sales_reps
   - Orders: orders, order_lines, order_attachments, order_revisions, order_workflow_states
   - Production: production_orders, production_assignments, production_comments, production_revisions
   - Invoicing: invoices, invoice_lines, payments, makegoods
   - Logs: daily_logs, traffic_logs, log_revisions
   - Tracks: tracks, voice_tracks, voice_track_slots, voice_track_audits
   - Scheduling: dayparts, daypart_categories, clock_templates, break_structures
   - Audio: audio_cuts, audio_versions, audio_deliveries, audio_qc_results
   - And 20+ more tables...

## ⚠️ Important Notes

- **No data preservation needed** - Starting fresh
- **Migration is irreversible** - The downgrade function drops all tables
- **All relationships preserved** - Foreign keys maintain referential integrity
- **UUIDs auto-generated** - PostgreSQL's `gen_random_uuid()` handles ID generation

## 🎯 After Migration

Once the migration completes successfully:

1. The database will have all tables with UUID primary keys
2. All application code is already updated to use UUIDs
3. You can start using the application immediately
4. New records will automatically get UUID primary keys

## 🔍 Verify Migration

After running the migration, you can verify:

```bash
# Check migration status
docker-compose exec api alembic current

# Check tables in database
docker-compose exec db psql -U librelog -d librelog -c "\dt"

# Verify a table has UUID primary key
docker-compose exec db psql -U librelog -d librelog -c "\d users"
```

The `users` table (and all others) should show `id` as type `uuid` with default `gen_random_uuid()`.

