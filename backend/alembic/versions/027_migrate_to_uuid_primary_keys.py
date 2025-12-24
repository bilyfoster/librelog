"""Migrate all tables to UUID primary keys

Revision ID: 027
Revises: 026
Create Date: 2025-01-XX

This migration drops all existing tables and recreates them with UUID primary keys.
All foreign key relationships are updated to use UUID types.
Since the database can be wiped, this is a clean migration.

IMPORTANT: Run export_settings.py and export_admin_users.py BEFORE running this migration.
"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = '027'
down_revision = ('023', '026')  # Merge both heads
branch_labels = None
depends_on = None


def upgrade():
    """
    Drop all existing tables and recreate them with UUID primary keys.
    Tables are dropped in reverse dependency order to avoid foreign key constraints.
    """
    
    # Drop all tables (CASCADE handles foreign key dependencies)
    # We drop all tables at once since we're doing a complete wipe
    # Note: We preserve alembic_version table for migration tracking
    op.execute("""
        DO $$ DECLARE
            r RECORD;
        BEGIN
            FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename != 'alembic_version') LOOP
                EXECUTE 'DROP TABLE IF EXISTS ' || quote_ident(r.tablename) || ' CASCADE';
            END LOOP;
        END $$;
    """)
    
    # Drop all enum types (they will be recreated)
    op.execute("""
        DO $$ DECLARE
            r RECORD;
        BEGIN
            FOR r IN (SELECT typname FROM pg_type WHERE typtype = 'e' AND typnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public')) LOOP
                EXECUTE 'DROP TYPE IF EXISTS ' || quote_ident(r.typname) || ' CASCADE';
            END LOOP;
        END $$;
    """)
    
    # Recreate enum types
    enum_types = [
        ("ordertype", "('LOCAL', 'NATIONAL', 'NETWORK', 'DIGITAL', 'NTR')"),
        ("billingcycle", "('WEEKLY', 'MONTHLY', 'ONE_SHOT', 'BIWEEKLY', 'QUARTERLY')"),
        ("invoicetype", "('STANDARD', 'COOP', 'POLITICAL')"),
        ("politicalclass", "('FEDERAL', 'STATE', 'LOCAL', 'ISSUE')"),
        ("revenuetype", "('SPOT', 'DIGITAL', 'TRADE', 'PROMOTION', 'NTR')"),
        ("selloutclass", "('ROS', 'FIXED_POSITION', 'BONUS', 'GUARANTEED')"),
        ("attachmenttype", "('CONTRACT', 'AUDIO', 'SCRIPT', 'CREATIVE', 'LEGAL', 'IO', 'EMAIL', 'OTHER')"),
        ("revisionreasoncode", "('PRICE_CHANGE', 'EXTENSION', 'CANCELLATION', 'LINE_ADDED', 'LINE_REMOVED', 'COPY_CHANGE', 'DATE_CHANGE', 'OTHER')"),
        ("workflowstate", "('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'TRAFFIC_READY', 'BILLING_READY', 'LOCKED', 'CANCELLED')"),
        ("copytype", "('NEW', 'TAG_UPDATE', 'RENEWAL', 'SEASONAL')"),
        ("revenuebucket", "('SPOT', 'DIGITAL', 'TRADE', 'PROMOTIONS')"),
        ("ratetype", "('ROS', 'DAYPART', 'PROGRAM', 'FIXED_TIME')"),
        ("orderstatus", "('DRAFT', 'PENDING', 'APPROVED', 'ACTIVE', 'COMPLETED', 'CANCELLED')"),
        ("approvalstatus", "('PENDING', 'APPROVED', 'REJECTED', 'NOT_REQUIRED')"),
        ("breakposition", "('A', 'B', 'C', 'D', 'E')"),
        ("daypart", "('MORNING_DRIVE', 'MIDDAY', 'AFTERNOON_DRIVE', 'EVENING', 'OVERNIGHT')"),
        ("spotstatus", "('SCHEDULED', 'AIRED', 'MISSED', 'MAKEGOOD')"),
        ("copystatus", "('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'ARCHIVED')"),
        ("copyapprovalstatus", "('PENDING', 'APPROVED', 'REJECTED', 'NOT_REQUIRED')"),
        ("productionordertype", "('STANDARD', 'RUSH', 'SPEC')"),
        ("productionorderstatus", "('DRAFT', 'IN_PROGRESS', 'PENDING_APPROVAL', 'APPROVED', 'COMPLETED', 'CANCELLED')"),
        ("assignmenttype", "('COPY', 'VOICE', 'PRODUCTION', 'APPROVAL')"),
        ("assignmentstatus", "('PENDING', 'IN_PROGRESS', 'COMPLETED', 'REJECTED')"),
        ("talenttype", "('VOICE_TALENT', 'PRODUCER', 'ENGINEER')"),
        ("talentrequeststatus", "('PENDING', 'ASSIGNED', 'COMPLETED', 'CANCELLED')"),
        ("invoicestatus", "('DRAFT', 'SENT', 'PAID', 'OVERDUE', 'CANCELLED')"),
        ("periodtype", "('DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')"),
        ("platform", "('SPOTIFY', 'APPLE_MUSIC', 'GOOGLE_PODCASTS', 'IHEART', 'TUNEIN', 'OTHER')"),
        ("digitalorderstatus", "('DRAFT', 'ACTIVE', 'PAUSED', 'COMPLETED', 'CANCELLED')"),
        ("webhooktype", "('HTTP', 'HTTPS')"),
        ("webhookevent", "('ORDER_CREATED', 'ORDER_UPDATED', 'ORDER_DELETED', 'SPOT_SCHEDULED', 'SPOT_AIRED', 'INVOICE_CREATED', 'INVOICE_PAID')"),
        ("notificationtype", "('EMAIL', 'IN_APP', 'BOTH')"),
        ("notificationstatus", "('PENDING', 'SENT', 'FAILED', 'READ')"),
        ("backuptype", "('FULL', 'INCREMENTAL', 'DIFFERENTIAL')"),
        ("backupstatus", "('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED')"),
        ("storageprovider", "('LOCAL', 'S3', 'AZURE', 'GCS')"),
        ("deliverymethod", "('EMAIL', 'FTP', 'SFTP', 'S3', 'AZURE', 'GCS', 'CDN')"),
        ("deliverystatus", "('PENDING', 'IN_PROGRESS', 'DELIVERED', 'FAILED')"),
        ("rotationtype", "('SEQUENTIAL', 'RANDOM', 'WEIGHTED', 'DAYPART')"),
        ("trafficlogtype", "('SPOT', 'MAKEGOOD', 'PROMO', 'PSA')"),
        ("breaktype", "('MUSIC', 'NEWS', 'WEATHER', 'TRAFFIC', 'COMMERCIAL', 'PROMO', 'PSA')"),
        ("voicetrackstatus", "('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'ARCHIVED')"),
    ]
    
    for enum_name, enum_values in enum_types:
        op.execute(f"""
            DO $$ BEGIN
                CREATE TYPE {enum_name} AS ENUM {enum_values};
            EXCEPTION
                WHEN duplicate_object THEN null;
            END $$;
        """)
    
    # Create users table with UUID primary key
    op.create_table(
        'users',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('username', sa.String(length=50), nullable=False, unique=True),
        sa.Column('password_hash', sa.String(length=255), nullable=False),
        sa.Column('role', sa.String(length=20), nullable=False, server_default='dj'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('last_login', sa.DateTime(timezone=True), nullable=True),
        sa.Column('last_activity', sa.DateTime(timezone=True), nullable=True),
        sa.Column('permissions', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.CheckConstraint("role IN ('admin', 'producer', 'dj', 'sales', 'production_director', 'voice_talent')", name='users_role_check'),
    )
    op.create_index(op.f('ix_users_id'), 'users', ['id'], unique=False)
    op.create_index(op.f('ix_users_username'), 'users', ['username'], unique=True)
    
    # Create settings table with UUID primary key
    op.create_table(
        'settings',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('category', sa.String(length=50), nullable=False),
        sa.Column('key', sa.String(length=100), nullable=False),
        sa.Column('value', sa.Text(), nullable=True),
        sa.Column('encrypted', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_settings_id'), 'settings', ['id'], unique=False)
    op.create_index(op.f('ix_settings_category'), 'settings', ['category'], unique=False)
    op.create_index(op.f('ix_settings_key'), 'settings', ['key'], unique=False)
    op.create_index('ix_settings_category_key', 'settings', ['category', 'key'], unique=True)
    
    # Create stations table with UUID primary key
    op.create_table(
        'stations',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('call_letters', sa.String(length=10), nullable=False, unique=True),
        sa.Column('frequency', sa.String(length=20), nullable=True),
        sa.Column('market', sa.String(length=255), nullable=True),
        sa.Column('format', sa.String(length=100), nullable=True),
        sa.Column('ownership', sa.String(length=255), nullable=True),
        sa.Column('contacts', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('rates', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('inventory_class', sa.String(length=50), nullable=True),
        sa.Column('libretime_config', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_stations_id'), 'stations', ['id'], unique=False)
    op.create_index(op.f('ix_stations_call_letters'), 'stations', ['call_letters'], unique=True)
    op.create_index(op.f('ix_stations_frequency'), 'stations', ['frequency'], unique=False)
    op.create_index(op.f('ix_stations_market'), 'stations', ['market'], unique=False)
    op.create_index(op.f('ix_stations_inventory_class'), 'stations', ['inventory_class'], unique=False)
    op.create_index(op.f('ix_stations_active'), 'stations', ['active'], unique=False)
    
    # Create clusters table with UUID primary key
    op.create_table(
        'clusters',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('name', sa.String(length=255), nullable=False, unique=True),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_clusters_id'), 'clusters', ['id'], unique=False)
    
    # Create junction table for station_clusters
    op.create_table(
        'station_clusters',
        sa.Column('station_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('stations.id', ondelete='CASCADE'), nullable=False),
        sa.Column('cluster_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('clusters.id', ondelete='CASCADE'), nullable=False),
        sa.PrimaryKeyConstraint('station_id', 'cluster_id')
    )
    
    # Create advertisers table with UUID primary key
    op.create_table(
        'advertisers',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('name', sa.String(length=255), nullable=False),
        sa.Column('contact_first_name', sa.String(length=100), nullable=True),
        sa.Column('contact_last_name', sa.String(length=100), nullable=True),
        sa.Column('email', sa.String(length=255), nullable=True),
        sa.Column('phone', sa.String(length=50), nullable=True),
        sa.Column('address', sa.Text(), nullable=True),
        sa.Column('tax_id', sa.String(length=50), nullable=True),
        sa.Column('payment_terms', sa.String(length=100), nullable=True),
        sa.Column('credit_limit', sa.Numeric(10, 2), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_advertisers_id'), 'advertisers', ['id'], unique=False)
    op.create_index(op.f('ix_advertisers_name'), 'advertisers', ['name'], unique=False)
    op.create_index(op.f('ix_advertisers_email'), 'advertisers', ['email'], unique=False)
    op.create_index(op.f('ix_advertisers_active'), 'advertisers', ['active'], unique=False)
    
    # Create agencies table with UUID primary key
    op.create_table(
        'agencies',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('name', sa.String(length=255), nullable=False, unique=True),
        sa.Column('contact_first_name', sa.String(length=100), nullable=True),
        sa.Column('contact_last_name', sa.String(length=100), nullable=True),
        sa.Column('email', sa.String(length=255), nullable=True),
        sa.Column('phone', sa.String(length=50), nullable=True),
        sa.Column('address', sa.Text(), nullable=True),
        sa.Column('commission_rate', sa.Numeric(5, 2), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_agencies_id'), 'agencies', ['id'], unique=False)
    op.create_index(op.f('ix_agencies_name'), 'agencies', ['name'], unique=True)
    op.create_index(op.f('ix_agencies_active'), 'agencies', ['active'], unique=False)
    
    # Create campaigns table with UUID primary key and UUID foreign keys
    op.create_table(
        'campaigns',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('advertiser', sa.String(length=255), nullable=False),
        sa.Column('start_date', sa.Date(), nullable=False),
        sa.Column('end_date', sa.Date(), nullable=False),
        sa.Column('priority', sa.Integer(), nullable=False, server_default='1'),
        sa.Column('file_url', sa.Text(), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('order_number', sa.String(length=50), nullable=True, unique=True),
        sa.Column('contract_number', sa.String(length=50), nullable=True),
        sa.Column('insertion_order_url', sa.Text(), nullable=True),
        sa.Column('spot_lengths', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('rate_type', postgresql.ENUM('ROS', 'DAYPART', 'PROGRAM', 'FIXED_TIME', name='ratetype', create_type=False), nullable=True),
        sa.Column('rates', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('scripts', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('copy_instructions', sa.Text(), nullable=True),
        sa.Column('traffic_restrictions', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('approval_status', postgresql.ENUM('PENDING', 'APPROVED', 'REJECTED', 'NOT_REQUIRED', name='approvalstatus', create_type=False), nullable=True, server_default='NOT_REQUIRED'),
        sa.Column('approved_by', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='SET NULL'), nullable=True),
        sa.Column('approved_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_campaigns_id'), 'campaigns', ['id'], unique=False)
    op.create_index(op.f('ix_campaigns_advertiser'), 'campaigns', ['advertiser'], unique=False)
    op.create_index(op.f('ix_campaigns_start_date'), 'campaigns', ['start_date'], unique=False)
    op.create_index(op.f('ix_campaigns_end_date'), 'campaigns', ['end_date'], unique=False)
    op.create_index(op.f('ix_campaigns_priority'), 'campaigns', ['priority'], unique=False)
    op.create_index(op.f('ix_campaigns_active'), 'campaigns', ['active'], unique=False)
    op.create_index(op.f('ix_campaigns_order_number'), 'campaigns', ['order_number'], unique=True)
    op.create_index(op.f('ix_campaigns_contract_number'), 'campaigns', ['contract_number'], unique=False)
    
    # Create sales_teams table with UUID primary key
    op.create_table(
        'sales_teams',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('name', sa.String(length=255), nullable=False, unique=True),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_sales_teams_id'), 'sales_teams', ['id'], unique=False)
    op.create_index(op.f('ix_sales_teams_name'), 'sales_teams', ['name'], unique=True)
    op.create_index(op.f('ix_sales_teams_active'), 'sales_teams', ['active'], unique=False)
    
    # Create sales_regions table with UUID primary key
    op.create_table(
        'sales_regions',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('name', sa.String(length=255), nullable=False, unique=True),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_sales_regions_id'), 'sales_regions', ['id'], unique=False)
    op.create_index(op.f('ix_sales_regions_name'), 'sales_regions', ['name'], unique=True)
    op.create_index(op.f('ix_sales_regions_active'), 'sales_regions', ['active'], unique=False)
    
    # Create sales_offices table with UUID primary key and UUID foreign keys
    op.create_table(
        'sales_offices',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('name', sa.String(length=255), nullable=False, unique=True),
        sa.Column('region_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('sales_regions.id', ondelete='SET NULL'), nullable=True),
        sa.Column('address', sa.Text(), nullable=True),
        sa.Column('phone', sa.String(length=50), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_sales_offices_id'), 'sales_offices', ['id'], unique=False)
    op.create_index(op.f('ix_sales_offices_name'), 'sales_offices', ['name'], unique=True)
    op.create_index(op.f('ix_sales_offices_region_id'), 'sales_offices', ['region_id'], unique=False)
    op.create_index(op.f('ix_sales_offices_active'), 'sales_offices', ['active'], unique=False)
    
    # Create sales_reps table with UUID primary key and UUID foreign keys
    op.create_table(
        'sales_reps',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('user_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='CASCADE'), nullable=False, unique=True),
        sa.Column('employee_id', sa.String(length=50), nullable=True, unique=True),
        sa.Column('commission_rate', sa.Numeric(5, 2), nullable=True),
        sa.Column('sales_goal', sa.Numeric(10, 2), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_sales_reps_id'), 'sales_reps', ['id'], unique=False)
    op.create_index(op.f('ix_sales_reps_user_id'), 'sales_reps', ['user_id'], unique=True)
    op.create_index(op.f('ix_sales_reps_employee_id'), 'sales_reps', ['employee_id'], unique=True)
    op.create_index(op.f('ix_sales_reps_active'), 'sales_reps', ['active'], unique=False)
    
    # Create junction tables for sales associations
    op.create_table(
        'sales_rep_teams',
        sa.Column('sales_rep_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('sales_reps.id', ondelete='CASCADE'), nullable=False),
        sa.Column('sales_team_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('sales_teams.id', ondelete='CASCADE'), nullable=False),
        sa.PrimaryKeyConstraint('sales_rep_id', 'sales_team_id')
    )
    op.create_table(
        'sales_rep_offices',
        sa.Column('sales_rep_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('sales_reps.id', ondelete='CASCADE'), nullable=False),
        sa.Column('sales_office_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('sales_offices.id', ondelete='CASCADE'), nullable=False),
        sa.PrimaryKeyConstraint('sales_rep_id', 'sales_office_id')
    )
    op.create_table(
        'sales_rep_regions',
        sa.Column('sales_rep_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('sales_reps.id', ondelete='CASCADE'), nullable=False),
        sa.Column('sales_region_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('sales_regions.id', ondelete='CASCADE'), nullable=False),
        sa.PrimaryKeyConstraint('sales_rep_id', 'sales_region_id')
    )
    
    # Create user_stations junction table
    op.create_table(
        'user_stations',
        sa.Column('user_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='CASCADE'), nullable=False),
        sa.Column('station_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('stations.id', ondelete='CASCADE'), nullable=False),
        sa.PrimaryKeyConstraint('user_id', 'station_id')
    )
    
    # NOTE: Due to the large number of remaining tables (50+), this migration file
    # needs to be completed with all remaining table definitions. The structure
    # is established above. Remaining tables include:
    # - orders (with all UUID foreign keys)
    # - order_lines, order_attachments, order_revisions, order_workflow_states, order_templates
    # - spots, copy, copy_assignments
    # - production_orders, production_assignments, production_comments, production_revisions
    # - invoices, invoice_lines, payments, makegoods
    # - tracks, voice_tracks, voice_track_slots, voice_track_audits, voice_talent_requests
    # - daily_logs, traffic_logs, log_revisions
    # - dayparts, daypart_categories, clock_templates, break_structures
    # - rotation_rules, inventory_slots
    # - audio_cuts, audio_versions, audio_deliveries, audio_qc_results
    # - live_reads, political_records
    # - notifications, audit_logs, webhooks, backups
    # - playback_history, digital_orders, sales_goals
    # - failed_login_attempts
    # - user_roles (junction table)
    # 
    # Create orders table with UUID primary key and UUID foreign keys
    op.create_table(
        'orders',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('order_number', sa.String(length=50), nullable=False, unique=True),
        sa.Column('order_name', sa.String(length=255), nullable=True),
        sa.Column('campaign_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('campaigns.id', ondelete='SET NULL'), nullable=True),
        sa.Column('advertiser_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('advertisers.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('agency_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('agencies.id', ondelete='SET NULL'), nullable=True),
        sa.Column('sales_rep_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('sales_reps.id', ondelete='SET NULL'), nullable=True),
        sa.Column('sales_team', sa.String(length=100), nullable=True),
        sa.Column('sales_office', sa.String(length=100), nullable=True),
        sa.Column('sales_region', sa.String(length=100), nullable=True),
        sa.Column('sales_team_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('sales_teams.id', ondelete='SET NULL'), nullable=True),
        sa.Column('sales_office_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('sales_offices.id', ondelete='SET NULL'), nullable=True),
        sa.Column('sales_region_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('sales_regions.id', ondelete='SET NULL'), nullable=True),
        sa.Column('stations', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('cluster', sa.String(length=100), nullable=True),
        sa.Column('order_type', postgresql.ENUM('LOCAL', 'NATIONAL', 'NETWORK', 'DIGITAL', 'NTR', name='ordertype', create_type=False), nullable=True),
        sa.Column('start_date', sa.Date(), nullable=False),
        sa.Column('end_date', sa.Date(), nullable=False),
        sa.Column('spot_lengths', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('total_spots', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('rate_type', postgresql.ENUM('ROS', 'DAYPART', 'PROGRAM', 'FIXED_TIME', name='ratetype', create_type=False), nullable=False, server_default='ROS'),
        sa.Column('rates', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('gross_amount', sa.Numeric(12, 2), nullable=False, server_default='0'),
        sa.Column('net_amount', sa.Numeric(12, 2), nullable=False, server_default='0'),
        sa.Column('total_value', sa.Numeric(12, 2), nullable=False, server_default='0'),
        sa.Column('agency_commission_percent', sa.Numeric(5, 2), nullable=True),
        sa.Column('agency_commission_amount', sa.Numeric(12, 2), nullable=True),
        sa.Column('agency_discount', sa.Numeric(12, 2), nullable=True),
        sa.Column('cash_discount', sa.Numeric(12, 2), nullable=True),
        sa.Column('trade_barter', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('trade_value', sa.Numeric(12, 2), nullable=True),
        sa.Column('taxable', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('billing_cycle', postgresql.ENUM('WEEKLY', 'MONTHLY', 'ONE_SHOT', 'BIWEEKLY', 'QUARTERLY', name='billingcycle', create_type=False), nullable=True),
        sa.Column('invoice_type', postgresql.ENUM('STANDARD', 'COOP', 'POLITICAL', name='invoicetype', create_type=False), nullable=True),
        sa.Column('coop_sponsor', sa.String(length=255), nullable=True),
        sa.Column('coop_percent', sa.Numeric(5, 2), nullable=True),
        sa.Column('client_po_number', sa.String(length=100), nullable=True),
        sa.Column('billing_address', sa.Text(), nullable=True),
        sa.Column('billing_contact', sa.String(length=255), nullable=True),
        sa.Column('billing_contact_email', sa.String(length=255), nullable=True),
        sa.Column('billing_contact_phone', sa.String(length=50), nullable=True),
        sa.Column('political_class', postgresql.ENUM('FEDERAL', 'STATE', 'LOCAL', 'ISSUE', name='politicalclass', create_type=False), nullable=True),
        sa.Column('political_window_flag', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('contract_reference', sa.String(length=100), nullable=True),
        sa.Column('insertion_order_number', sa.String(length=100), nullable=True),
        sa.Column('regulatory_notes', sa.Text(), nullable=True),
        sa.Column('fcc_id', sa.String(length=100), nullable=True),
        sa.Column('required_disclaimers', sa.Text(), nullable=True),
        sa.Column('status', postgresql.ENUM('DRAFT', 'PENDING', 'APPROVED', 'ACTIVE', 'COMPLETED', 'CANCELLED', name='orderstatus', create_type=False), nullable=False, server_default='DRAFT'),
        sa.Column('approval_status', postgresql.ENUM('PENDING', 'APPROVED', 'REJECTED', 'NOT_REQUIRED', name='approvalstatus', create_type=False), nullable=False, server_default='NOT_REQUIRED'),
        sa.Column('approved_by', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='SET NULL'), nullable=True),
        sa.Column('approved_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('traffic_ready', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('billing_ready', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('locked', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('revision_number', sa.Integer(), nullable=False, server_default='1'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('created_by', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='SET NULL'), nullable=True),
    )
    op.create_index(op.f('ix_orders_id'), 'orders', ['id'], unique=False)
    op.create_index(op.f('ix_orders_order_number'), 'orders', ['order_number'], unique=True)
    op.create_index(op.f('ix_orders_order_name'), 'orders', ['order_name'], unique=False)
    op.create_index(op.f('ix_orders_campaign_id'), 'orders', ['campaign_id'], unique=False)
    op.create_index(op.f('ix_orders_advertiser_id'), 'orders', ['advertiser_id'], unique=False)
    op.create_index(op.f('ix_orders_agency_id'), 'orders', ['agency_id'], unique=False)
    op.create_index(op.f('ix_orders_sales_rep_id'), 'orders', ['sales_rep_id'], unique=False)
    op.create_index(op.f('ix_orders_sales_team_id'), 'orders', ['sales_team_id'], unique=False)
    op.create_index(op.f('ix_orders_sales_office_id'), 'orders', ['sales_office_id'], unique=False)
    op.create_index(op.f('ix_orders_sales_region_id'), 'orders', ['sales_region_id'], unique=False)
    op.create_index(op.f('ix_orders_order_type'), 'orders', ['order_type'], unique=False)
    op.create_index(op.f('ix_orders_start_date'), 'orders', ['start_date'], unique=False)
    op.create_index(op.f('ix_orders_end_date'), 'orders', ['end_date'], unique=False)
    op.create_index(op.f('ix_orders_trade_barter'), 'orders', ['trade_barter'], unique=False)
    op.create_index(op.f('ix_orders_taxable'), 'orders', ['taxable'], unique=False)
    op.create_index(op.f('ix_orders_client_po_number'), 'orders', ['client_po_number'], unique=False)
    op.create_index(op.f('ix_orders_political_class'), 'orders', ['political_class'], unique=False)
    op.create_index(op.f('ix_orders_political_window_flag'), 'orders', ['political_window_flag'], unique=False)
    op.create_index(op.f('ix_orders_contract_reference'), 'orders', ['contract_reference'], unique=False)
    op.create_index(op.f('ix_orders_insertion_order_number'), 'orders', ['insertion_order_number'], unique=False)
    op.create_index(op.f('ix_orders_status'), 'orders', ['status'], unique=False)
    op.create_index(op.f('ix_orders_traffic_ready'), 'orders', ['traffic_ready'], unique=False)
    op.create_index(op.f('ix_orders_billing_ready'), 'orders', ['billing_ready'], unique=False)
    op.create_index(op.f('ix_orders_locked'), 'orders', ['locked'], unique=False)
    
    # Create order_lines table with UUID primary key and UUID foreign keys
    op.create_table(
        'order_lines',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('order_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('orders.id', ondelete='CASCADE'), nullable=False),
        sa.Column('line_number', sa.Integer(), nullable=False),
        sa.Column('start_date', sa.Date(), nullable=False),
        sa.Column('end_date', sa.Date(), nullable=False),
        sa.Column('station_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('stations.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('station', sa.String(length=100), nullable=True),
        sa.Column('product', sa.String(length=255), nullable=True),
        sa.Column('revenue_type', postgresql.ENUM('SPOT', 'DIGITAL', 'TRADE', 'PROMOTION', 'NTR', name='revenuetype', create_type=False), nullable=True),
        sa.Column('length', sa.Integer(), nullable=True),
        sa.Column('daypart', sa.String(length=50), nullable=True),
        sa.Column('days_of_week', sa.String(length=7), nullable=True),
        sa.Column('rate', sa.Numeric(12, 2), nullable=True),
        sa.Column('rate_type', sa.String(length=50), nullable=True),
        sa.Column('sellout_class', postgresql.ENUM('ROS', 'FIXED_POSITION', 'BONUS', 'GUARANTEED', name='selloutclass', create_type=False), nullable=True),
        sa.Column('priority_code', sa.String(length=50), nullable=True),
        sa.Column('spot_frequency', sa.Integer(), nullable=True),
        sa.Column('estimated_impressions', sa.Integer(), nullable=True),
        sa.Column('cpm', sa.Numeric(10, 2), nullable=True),
        sa.Column('cpp', sa.Numeric(10, 2), nullable=True),
        sa.Column('makegood_eligible', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('guaranteed_position', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('preemptible', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('inventory_class', sa.String(length=100), nullable=True),
        sa.Column('product_code', sa.String(length=100), nullable=True),
        sa.Column('deal_points', sa.Text(), nullable=True),
        sa.Column('notes', sa.Text(), nullable=True),
        sa.Column('platform', sa.String(length=100), nullable=True),
        sa.Column('impressions_booked', sa.Integer(), nullable=True),
        sa.Column('delivery_window', sa.String(length=100), nullable=True),
        sa.Column('targeting_parameters', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('companion_banners', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_order_lines_id'), 'order_lines', ['id'], unique=False)
    op.create_index(op.f('ix_order_lines_order_id'), 'order_lines', ['order_id'], unique=False)
    op.create_index(op.f('ix_order_lines_start_date'), 'order_lines', ['start_date'], unique=False)
    op.create_index(op.f('ix_order_lines_end_date'), 'order_lines', ['end_date'], unique=False)
    op.create_index(op.f('ix_order_lines_station_id'), 'order_lines', ['station_id'], unique=False)
    op.create_index(op.f('ix_order_lines_station'), 'order_lines', ['station'], unique=False)
    op.create_index(op.f('ix_order_lines_daypart'), 'order_lines', ['daypart'], unique=False)
    op.create_index(op.f('ix_order_lines_revenue_type'), 'order_lines', ['revenue_type'], unique=False)
    op.create_index(op.f('ix_order_lines_priority_code'), 'order_lines', ['priority_code'], unique=False)
    op.create_index(op.f('ix_order_lines_product_code'), 'order_lines', ['product_code'], unique=False)
    op.create_index(op.f('ix_order_lines_makegood_eligible'), 'order_lines', ['makegood_eligible'], unique=False)
    op.create_index(op.f('ix_order_lines_guaranteed_position'), 'order_lines', ['guaranteed_position'], unique=False)
    op.create_index(op.f('ix_order_lines_preemptible'), 'order_lines', ['preemptible'], unique=False)
    
    # Create spots table with UUID primary key and UUID foreign keys
    op.create_table(
        'spots',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('order_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('orders.id', ondelete='CASCADE'), nullable=False),
        sa.Column('campaign_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('campaigns.id', ondelete='SET NULL'), nullable=True),
        sa.Column('station_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('stations.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('scheduled_date', sa.Date(), nullable=False),
        sa.Column('scheduled_time', sa.String(length=8), nullable=False),
        sa.Column('spot_length', sa.Integer(), nullable=False),
        sa.Column('break_position', postgresql.ENUM('A', 'B', 'C', 'D', 'E', name='breakposition', create_type=False), nullable=True),
        sa.Column('daypart', postgresql.ENUM('MORNING_DRIVE', 'MIDDAY', 'AFTERNOON_DRIVE', 'EVENING', 'OVERNIGHT', name='daypart', create_type=False), nullable=True),
        sa.Column('status', postgresql.ENUM('SCHEDULED', 'AIRED', 'MISSED', 'MAKEGOOD', name='spotstatus', create_type=False), nullable=False, server_default='SCHEDULED'),
        sa.Column('actual_air_time', sa.DateTime(timezone=True), nullable=True),
        sa.Column('makegood_of_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('spots.id', ondelete='SET NULL'), nullable=True),
        sa.Column('conflict_resolved', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_spots_id'), 'spots', ['id'], unique=False)
    op.create_index(op.f('ix_spots_order_id'), 'spots', ['order_id'], unique=False)
    op.create_index(op.f('ix_spots_campaign_id'), 'spots', ['campaign_id'], unique=False)
    op.create_index(op.f('ix_spots_station_id'), 'spots', ['station_id'], unique=False)
    op.create_index(op.f('ix_spots_scheduled_date'), 'spots', ['scheduled_date'], unique=False)
    op.create_index(op.f('ix_spots_daypart'), 'spots', ['daypart'], unique=False)
    op.create_index(op.f('ix_spots_status'), 'spots', ['status'], unique=False)
    
    # Create copy table with UUID primary key and UUID foreign keys
    op.create_table(
        'copy',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('order_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('orders.id', ondelete='SET NULL'), nullable=True),
        sa.Column('advertiser_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('advertisers.id', ondelete='SET NULL'), nullable=True),
        sa.Column('title', sa.String(length=255), nullable=False),
        sa.Column('copy_code', sa.String(length=100), nullable=True),
        sa.Column('isci_code', sa.String(length=100), nullable=True),
        sa.Column('copy_type', postgresql.ENUM('NEW', 'TAG_UPDATE', 'RENEWAL', 'SEASONAL', name='copytype', create_type=False), nullable=True),
        sa.Column('script_text', sa.Text(), nullable=True),
        sa.Column('audio_file_path', sa.Text(), nullable=True),
        sa.Column('audio_file_url', sa.Text(), nullable=True),
        sa.Column('client_provided_audio', sa.Text(), nullable=True),
        sa.Column('version', sa.Integer(), nullable=False, server_default='1'),
        sa.Column('effective_date', sa.Date(), nullable=True),
        sa.Column('expires_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('expiration_date', sa.Date(), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('priority', sa.Integer(), nullable=True),
        sa.Column('rotation_mode', sa.String(length=50), nullable=True),
        sa.Column('cut_count', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('political_flag', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('live_read_enabled', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('copy_instructions', sa.Text(), nullable=True),
        sa.Column('instruction_attachments', sa.Text(), nullable=True),
        sa.Column('production_notes', sa.Text(), nullable=True),
        sa.Column('legal_copy', sa.Text(), nullable=True),
        sa.Column('required_disclaimers', sa.Text(), nullable=True),
        sa.Column('talent_restrictions', sa.Text(), nullable=True),
        sa.Column('aqh_reconciliation', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('production_order_id', postgresql.UUID(as_uuid=True), nullable=True, unique=True),
        sa.Column('copy_status', postgresql.ENUM('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'ARCHIVED', name='copystatus', create_type=False), nullable=False, server_default='DRAFT'),
        sa.Column('needs_production', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('copy_approval_status', postgresql.ENUM('PENDING', 'APPROVED', 'REJECTED', 'NOT_REQUIRED', name='copyapprovalstatus', create_type=False), nullable=False, server_default='PENDING'),
        sa.Column('script_approved_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('script_approved_by', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='SET NULL'), nullable=True),
        sa.Column('audio_format', sa.String(length=50), nullable=True),
        sa.Column('audio_source', sa.String(length=100), nullable=True),
        sa.Column('loudness_check', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_copy_id'), 'copy', ['id'], unique=False)
    op.create_index(op.f('ix_copy_order_id'), 'copy', ['order_id'], unique=False)
    op.create_index(op.f('ix_copy_advertiser_id'), 'copy', ['advertiser_id'], unique=False)
    op.create_index(op.f('ix_copy_title'), 'copy', ['title'], unique=False)
    op.create_index(op.f('ix_copy_copy_code'), 'copy', ['copy_code'], unique=False)
    op.create_index(op.f('ix_copy_isci_code'), 'copy', ['isci_code'], unique=False)
    op.create_index(op.f('ix_copy_copy_type'), 'copy', ['copy_type'], unique=False)
    op.create_index(op.f('ix_copy_effective_date'), 'copy', ['effective_date'], unique=False)
    op.create_index(op.f('ix_copy_expires_at'), 'copy', ['expires_at'], unique=False)
    op.create_index(op.f('ix_copy_expiration_date'), 'copy', ['expiration_date'], unique=False)
    op.create_index(op.f('ix_copy_active'), 'copy', ['active'], unique=False)
    op.create_index(op.f('ix_copy_production_order_id'), 'copy', ['production_order_id'], unique=True)
    op.create_index(op.f('ix_copy_copy_status'), 'copy', ['copy_status'], unique=False)
    op.create_index(op.f('ix_copy_needs_production'), 'copy', ['needs_production'], unique=False)
    op.create_index(op.f('ix_copy_political_flag'), 'copy', ['political_flag'], unique=False)
    op.create_index(op.f('ix_copy_live_read_enabled'), 'copy', ['live_read_enabled'], unique=False)
    op.create_index(op.f('ix_copy_loudness_check'), 'copy', ['loudness_check'], unique=False)
    
    # Create production_orders table with UUID primary key and UUID foreign keys
    op.create_table(
        'production_orders',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('po_number', sa.String(length=50), nullable=False, unique=True),
        sa.Column('copy_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('copy.id', ondelete='CASCADE'), nullable=False, unique=True),
        sa.Column('order_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('orders.id', ondelete='SET NULL'), nullable=True),
        sa.Column('campaign_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('campaigns.id', ondelete='SET NULL'), nullable=True),
        sa.Column('advertiser_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('advertisers.id', ondelete='SET NULL'), nullable=True),
        sa.Column('client_name', sa.String(length=255), nullable=False),
        sa.Column('campaign_title', sa.String(length=255), nullable=True),
        sa.Column('start_date', sa.Date(), nullable=True),
        sa.Column('end_date', sa.Date(), nullable=True),
        sa.Column('budget', sa.Numeric(10, 2), nullable=True),
        sa.Column('contract_number', sa.String(length=100), nullable=True),
        sa.Column('spot_lengths', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('deliverables', sa.Text(), nullable=True),
        sa.Column('copy_requirements', sa.Text(), nullable=True),
        sa.Column('talent_needs', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('audio_references', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('instructions', sa.Text(), nullable=True),
        sa.Column('deadline', sa.DateTime(timezone=True), nullable=True),
        sa.Column('stations', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('version_count', sa.Integer(), nullable=False, server_default='1'),
        sa.Column('order_type', postgresql.ENUM('STANDARD', 'RUSH', 'SPEC', name='productionordertype', create_type=False), nullable=False, server_default='STANDARD'),
        sa.Column('status', postgresql.ENUM('DRAFT', 'IN_PROGRESS', 'PENDING_APPROVAL', 'APPROVED', 'COMPLETED', 'CANCELLED', name='productionorderstatus', create_type=False), nullable=False, server_default='DRAFT'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('completed_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('delivered_at', sa.DateTime(timezone=True), nullable=True),
    )
    op.create_index(op.f('ix_production_orders_id'), 'production_orders', ['id'], unique=False)
    op.create_index(op.f('ix_production_orders_po_number'), 'production_orders', ['po_number'], unique=True)
    op.create_index(op.f('ix_production_orders_copy_id'), 'production_orders', ['copy_id'], unique=True)
    op.create_index(op.f('ix_production_orders_order_id'), 'production_orders', ['order_id'], unique=False)
    op.create_index(op.f('ix_production_orders_campaign_id'), 'production_orders', ['campaign_id'], unique=False)
    op.create_index(op.f('ix_production_orders_advertiser_id'), 'production_orders', ['advertiser_id'], unique=False)
    op.create_index(op.f('ix_production_orders_deadline'), 'production_orders', ['deadline'], unique=False)
    op.create_index(op.f('ix_production_orders_status'), 'production_orders', ['status'], unique=False)
    
    # Create order_attachments table with UUID primary key and UUID foreign keys
    op.create_table(
        'order_attachments',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('order_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('orders.id', ondelete='CASCADE'), nullable=True),
        sa.Column('order_line_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('order_lines.id', ondelete='CASCADE'), nullable=True),
        sa.Column('copy_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('copy.id', ondelete='CASCADE'), nullable=True),
        sa.Column('file_path', sa.Text(), nullable=False),
        sa.Column('file_name', sa.String(length=255), nullable=False),
        sa.Column('mime_type', sa.String(length=100), nullable=True),
        sa.Column('file_size', sa.BigInteger(), nullable=True),
        sa.Column('attachment_type', postgresql.ENUM('CONTRACT', 'AUDIO', 'SCRIPT', 'CREATIVE', 'LEGAL', 'IO', 'EMAIL', 'OTHER', name='attachmenttype', create_type=False), nullable=False),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('notes', sa.Text(), nullable=True),
        sa.Column('uploaded_by', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('uploaded_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_order_attachments_id'), 'order_attachments', ['id'], unique=False)
    op.create_index(op.f('ix_order_attachments_order_id'), 'order_attachments', ['order_id'], unique=False)
    op.create_index(op.f('ix_order_attachments_order_line_id'), 'order_attachments', ['order_line_id'], unique=False)
    op.create_index(op.f('ix_order_attachments_copy_id'), 'order_attachments', ['copy_id'], unique=False)
    op.create_index(op.f('ix_order_attachments_attachment_type'), 'order_attachments', ['attachment_type'], unique=False)
    op.create_index(op.f('ix_order_attachments_uploaded_at'), 'order_attachments', ['uploaded_at'], unique=False)
    
    # Create order_revisions table with UUID primary key and UUID foreign keys
    op.create_table(
        'order_revisions',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('order_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('orders.id', ondelete='CASCADE'), nullable=False),
        sa.Column('revision_number', sa.Integer(), nullable=False),
        sa.Column('changed_fields', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('reason_code', postgresql.ENUM('PRICE_CHANGE', 'EXTENSION', 'CANCELLATION', 'LINE_ADDED', 'LINE_REMOVED', 'COPY_CHANGE', 'DATE_CHANGE', 'OTHER', name='revisionreasoncode', create_type=False), nullable=True),
        sa.Column('reason_notes', sa.Text(), nullable=True),
        sa.Column('approval_status_at_revision', sa.String(length=50), nullable=True),
        sa.Column('changed_by', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('changed_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_order_revisions_id'), 'order_revisions', ['id'], unique=False)
    op.create_index(op.f('ix_order_revisions_order_id'), 'order_revisions', ['order_id'], unique=False)
    op.create_index(op.f('ix_order_revisions_revision_number'), 'order_revisions', ['revision_number'], unique=False)
    op.create_index(op.f('ix_order_revisions_reason_code'), 'order_revisions', ['reason_code'], unique=False)
    op.create_index(op.f('ix_order_revisions_changed_at'), 'order_revisions', ['changed_at'], unique=False)
    
    # Create invoices table with UUID primary key and UUID foreign keys
    op.create_table(
        'invoices',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('invoice_number', sa.String(length=50), nullable=False, unique=True),
        sa.Column('advertiser_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('advertisers.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('agency_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('agencies.id', ondelete='SET NULL'), nullable=True),
        sa.Column('order_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('orders.id', ondelete='SET NULL'), nullable=True),
        sa.Column('campaign_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('campaigns.id', ondelete='SET NULL'), nullable=True),
        sa.Column('invoice_date', sa.Date(), nullable=False),
        sa.Column('due_date', sa.Date(), nullable=False),
        sa.Column('subtotal', sa.Numeric(10, 2), nullable=False, server_default='0'),
        sa.Column('tax', sa.Numeric(10, 2), nullable=False, server_default='0'),
        sa.Column('total', sa.Numeric(10, 2), nullable=False, server_default='0'),
        sa.Column('status', postgresql.ENUM('DRAFT', 'SENT', 'PAID', 'OVERDUE', 'CANCELLED', name='invoicestatus', create_type=False), nullable=False, server_default='DRAFT'),
        sa.Column('payment_terms', sa.String(length=100), nullable=True),
        sa.Column('notes', sa.String(length=500), nullable=True),
        sa.Column('billing_start', sa.Date(), nullable=True),
        sa.Column('billing_end', sa.Date(), nullable=True),
        sa.Column('billing_schedule', sa.Text(), nullable=True),
        sa.Column('billing_notes', sa.Text(), nullable=True),
        sa.Column('invoice_grouping_code', sa.String(length=100), nullable=True),
        sa.Column('revenue_class', sa.String(length=100), nullable=True),
        sa.Column('gl_account', sa.String(length=100), nullable=True),
        sa.Column('revenue_bucket', postgresql.ENUM('SPOT', 'DIGITAL', 'TRADE', 'PROMOTIONS', name='revenuebucket', create_type=False), nullable=True),
        sa.Column('prepaid', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('credits_issued', sa.Numeric(10, 2), nullable=False, server_default='0'),
        sa.Column('adjustments', sa.Numeric(10, 2), nullable=False, server_default='0'),
        sa.Column('sponsorship_fee', sa.Numeric(10, 2), nullable=True),
        sa.Column('election_cycle', sa.String(length=100), nullable=True),
        sa.Column('lowest_unit_rate_record', sa.Text(), nullable=True),
        sa.Column('class_comparison', sa.Text(), nullable=True),
        sa.Column('window_dates', sa.Text(), nullable=True),
        sa.Column('substantiation_docs', sa.Text(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_invoices_id'), 'invoices', ['id'], unique=False)
    op.create_index(op.f('ix_invoices_invoice_number'), 'invoices', ['invoice_number'], unique=True)
    op.create_index(op.f('ix_invoices_advertiser_id'), 'invoices', ['advertiser_id'], unique=False)
    op.create_index(op.f('ix_invoices_agency_id'), 'invoices', ['agency_id'], unique=False)
    op.create_index(op.f('ix_invoices_order_id'), 'invoices', ['order_id'], unique=False)
    op.create_index(op.f('ix_invoices_campaign_id'), 'invoices', ['campaign_id'], unique=False)
    op.create_index(op.f('ix_invoices_invoice_date'), 'invoices', ['invoice_date'], unique=False)
    op.create_index(op.f('ix_invoices_due_date'), 'invoices', ['due_date'], unique=False)
    op.create_index(op.f('ix_invoices_status'), 'invoices', ['status'], unique=False)
    op.create_index(op.f('ix_invoices_invoice_grouping_code'), 'invoices', ['invoice_grouping_code'], unique=False)
    op.create_index(op.f('ix_invoices_revenue_bucket'), 'invoices', ['revenue_bucket'], unique=False)
    op.create_index(op.f('ix_invoices_prepaid'), 'invoices', ['prepaid'], unique=False)
    op.create_index(op.f('ix_invoices_billing_start'), 'invoices', ['billing_start'], unique=False)
    op.create_index(op.f('ix_invoices_billing_end'), 'invoices', ['billing_end'], unique=False)
    
    # Create invoice_lines table with UUID primary key and UUID foreign keys
    op.create_table(
        'invoice_lines',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('invoice_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('invoices.id', ondelete='CASCADE'), nullable=False),
        sa.Column('station_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('stations.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('description', sa.String(length=255), nullable=False),
        sa.Column('quantity', sa.Integer(), nullable=False, server_default='1'),
        sa.Column('unit_price', sa.Numeric(10, 2), nullable=False),
        sa.Column('total', sa.Numeric(10, 2), nullable=False),
        sa.Column('spot_ids', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_invoice_lines_id'), 'invoice_lines', ['id'], unique=False)
    op.create_index(op.f('ix_invoice_lines_invoice_id'), 'invoice_lines', ['invoice_id'], unique=False)
    op.create_index(op.f('ix_invoice_lines_station_id'), 'invoice_lines', ['station_id'], unique=False)
    
    # Create copy_assignments table with UUID primary key and UUID foreign keys
    op.create_table(
        'copy_assignments',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('spot_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('spots.id', ondelete='CASCADE'), nullable=False),
        sa.Column('copy_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('copy.id', ondelete='CASCADE'), nullable=False),
        sa.Column('order_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('orders.id', ondelete='SET NULL'), nullable=True),
        sa.Column('assigned_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('assigned_by', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='SET NULL'), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_copy_assignments_id'), 'copy_assignments', ['id'], unique=False)
    op.create_index(op.f('ix_copy_assignments_spot_id'), 'copy_assignments', ['spot_id'], unique=False)
    op.create_index(op.f('ix_copy_assignments_copy_id'), 'copy_assignments', ['copy_id'], unique=False)
    op.create_index(op.f('ix_copy_assignments_order_id'), 'copy_assignments', ['order_id'], unique=False)
    
    # Create production_assignments table with UUID primary key and UUID foreign keys
    op.create_table(
        'production_assignments',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('production_order_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('production_orders.id', ondelete='CASCADE'), nullable=False),
        sa.Column('user_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('assignment_type', postgresql.ENUM('producer', 'voice_talent', 'imaging', 'qc', 'production_director', name='assignmenttype', create_type=False), nullable=False),
        sa.Column('status', postgresql.ENUM('PENDING', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED', 'REJECTED', name='assignmentstatus', create_type=False), nullable=False, server_default='PENDING'),
        sa.Column('notes', sa.Text(), nullable=True),
        sa.Column('assigned_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('accepted_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('completed_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_production_assignments_id'), 'production_assignments', ['id'], unique=False)
    op.create_index(op.f('ix_production_assignments_production_order_id'), 'production_assignments', ['production_order_id'], unique=False)
    op.create_index(op.f('ix_production_assignments_user_id'), 'production_assignments', ['user_id'], unique=False)
    op.create_index(op.f('ix_production_assignments_assignment_type'), 'production_assignments', ['assignment_type'], unique=False)
    op.create_index(op.f('ix_production_assignments_status'), 'production_assignments', ['status'], unique=False)
    
    # Create failed_login_attempts table with UUID primary key
    op.create_table(
        'failed_login_attempts',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('username', sa.String(length=50), nullable=False),
        sa.Column('ip_address', sa.String(length=45), nullable=True),
        sa.Column('user_agent', sa.String(length=255), nullable=True),
        sa.Column('attempted_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_failed_login_attempts_id'), 'failed_login_attempts', ['id'], unique=False)
    op.create_index(op.f('ix_failed_login_attempts_username'), 'failed_login_attempts', ['username'], unique=False)
    op.create_index('idx_username_attempted_at', 'failed_login_attempts', ['username', 'attempted_at'], unique=False)
    
    # Create tracks table with UUID primary key and UUID foreign keys
    op.create_table(
        'tracks',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('title', sa.String(length=255), nullable=False),
        sa.Column('artist', sa.String(length=255), nullable=True),
        sa.Column('album', sa.String(length=255), nullable=True),
        sa.Column('type', sa.String(length=10), nullable=False),
        sa.Column('genre', sa.String(length=100), nullable=True),
        sa.Column('duration', sa.Integer(), nullable=True),
        sa.Column('filepath', sa.Text(), nullable=False),
        sa.Column('libretime_id', sa.String(length=50), nullable=True, unique=True),
        sa.Column('last_played', sa.DateTime(timezone=True), nullable=True),
        sa.Column('bpm', sa.Integer(), nullable=True),
        sa.Column('daypart_eligible', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('is_new_release', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('allow_back_to_back', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('station_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('stations.id', ondelete='SET NULL'), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.CheckConstraint("type IN ('MUS', 'ADV', 'PSA', 'LIN', 'INT', 'PRO', 'SHO', 'IDS', 'COM', 'NEW', 'VOT')", name='tracks_type_check'),
    )
    op.create_index(op.f('ix_tracks_id'), 'tracks', ['id'], unique=False)
    op.create_index(op.f('ix_tracks_title'), 'tracks', ['title'], unique=False)
    op.create_index(op.f('ix_tracks_artist'), 'tracks', ['artist'], unique=False)
    op.create_index(op.f('ix_tracks_type'), 'tracks', ['type'], unique=False)
    op.create_index(op.f('ix_tracks_genre'), 'tracks', ['genre'], unique=False)
    op.create_index(op.f('ix_tracks_libretime_id'), 'tracks', ['libretime_id'], unique=True)
    op.create_index(op.f('ix_tracks_bpm'), 'tracks', ['bpm'], unique=False)
    op.create_index(op.f('ix_tracks_is_new_release'), 'tracks', ['is_new_release'], unique=False)
    op.create_index(op.f('ix_tracks_station_id'), 'tracks', ['station_id'], unique=False)
    
    # Create voice_tracks table with UUID primary key and UUID foreign keys
    op.create_table(
        'voice_tracks',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('show_name', sa.String(length=255), nullable=True),
        sa.Column('file_url', sa.Text(), nullable=False),
        sa.Column('scheduled_time', sa.DateTime(timezone=True), nullable=True),
        sa.Column('uploaded_by', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('libretime_id', sa.String(length=50), nullable=True),
        sa.Column('standardized_name', sa.String(length=50), nullable=True),
        sa.Column('recorded_date', sa.DateTime(timezone=True), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('break_id', sa.Integer(), nullable=True),
        sa.Column('playlist_id', sa.Integer(), nullable=True),
        sa.Column('break_type', postgresql.ENUM('standard', 'legal_id', 'imaging', name='breaktype', create_type=False), nullable=True),
        sa.Column('slot_position', sa.String(length=10), nullable=True),
        sa.Column('ramp_time', sa.Numeric(5, 2), nullable=True),
        sa.Column('back_time', sa.Numeric(5, 2), nullable=True),
        sa.Column('take_number', sa.Integer(), nullable=False, server_default='1'),
        sa.Column('is_final', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('status', postgresql.ENUM('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'ARCHIVED', name='voicetrackstatus', create_type=False), nullable=False, server_default='DRAFT'),
        sa.Column('ducking_threshold', sa.Numeric(5, 2), nullable=False, server_default='-18.0'),
        sa.Column('raw_file_url', sa.Text(), nullable=True),
        sa.Column('mixed_file_url', sa.Text(), nullable=True),
        sa.Column('track_metadata', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_voice_tracks_id'), 'voice_tracks', ['id'], unique=False)
    op.create_index(op.f('ix_voice_tracks_show_name'), 'voice_tracks', ['show_name'], unique=False)
    op.create_index(op.f('ix_voice_tracks_scheduled_time'), 'voice_tracks', ['scheduled_time'], unique=False)
    op.create_index(op.f('ix_voice_tracks_libretime_id'), 'voice_tracks', ['libretime_id'], unique=False)
    op.create_index(op.f('ix_voice_tracks_standardized_name'), 'voice_tracks', ['standardized_name'], unique=False)
    op.create_index(op.f('ix_voice_tracks_recorded_date'), 'voice_tracks', ['recorded_date'], unique=False)
    op.create_index(op.f('ix_voice_tracks_break_id'), 'voice_tracks', ['break_id'], unique=False)
    op.create_index(op.f('ix_voice_tracks_playlist_id'), 'voice_tracks', ['playlist_id'], unique=False)
    op.create_index(op.f('ix_voice_tracks_is_final'), 'voice_tracks', ['is_final'], unique=False)
    op.create_index(op.f('ix_voice_tracks_status'), 'voice_tracks', ['status'], unique=False)
    
    # Create daily_logs table with UUID primary key and UUID foreign keys
    op.create_table(
        'daily_logs',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('date', sa.Date(), nullable=False),
        sa.Column('station_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('stations.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('generated_by', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('json_data', postgresql.JSONB(astext_type=sa.Text()), nullable=False),
        sa.Column('published', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('locked', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('locked_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('locked_by', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='SET NULL'), nullable=True),
        sa.Column('conflicts', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('oversell_warnings', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.UniqueConstraint('date', 'station_id', name='uq_daily_logs_date_station'),
    )
    op.create_index(op.f('ix_daily_logs_id'), 'daily_logs', ['id'], unique=False)
    op.create_index(op.f('ix_daily_logs_date'), 'daily_logs', ['date'], unique=False)
    op.create_index(op.f('ix_daily_logs_station_id'), 'daily_logs', ['station_id'], unique=False)
    op.create_index(op.f('ix_daily_logs_published'), 'daily_logs', ['published'], unique=False)
    op.create_index(op.f('ix_daily_logs_locked'), 'daily_logs', ['locked'], unique=False)
    
    # Create traffic_logs table with UUID primary key and UUID foreign keys
    op.create_table(
        'traffic_logs',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('log_type', postgresql.ENUM('SPOT_SCHEDULED', 'SPOT_MOVED', 'SPOT_DELETED', 'LOG_LOCKED', 'LOG_UNLOCKED', 'LOG_PUBLISHED', 'CONFLICT_DETECTED', 'CONFLICT_RESOLVED', 'COPY_ASSIGNED', 'COPY_UNASSIGNED', 'ORDER_CREATED', 'ORDER_UPDATED', 'ORDER_APPROVED', 'ORDER_CANCELLED', name='trafficlogtype', create_type=False), nullable=False),
        sa.Column('log_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('daily_logs.id', ondelete='SET NULL'), nullable=True),
        sa.Column('spot_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('spots.id', ondelete='SET NULL'), nullable=True),
        sa.Column('order_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('orders.id', ondelete='SET NULL'), nullable=True),
        sa.Column('campaign_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('campaigns.id', ondelete='SET NULL'), nullable=True),
        sa.Column('copy_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('copy.id', ondelete='SET NULL'), nullable=True),
        sa.Column('user_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('message', sa.Text(), nullable=False),
        sa.Column('meta_data', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('ip_address', sa.String(length=45), nullable=True),
        sa.Column('user_agent', sa.Text(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_traffic_logs_id'), 'traffic_logs', ['id'], unique=False)
    op.create_index(op.f('ix_traffic_logs_log_type'), 'traffic_logs', ['log_type'], unique=False)
    op.create_index(op.f('ix_traffic_logs_log_id'), 'traffic_logs', ['log_id'], unique=False)
    op.create_index(op.f('ix_traffic_logs_spot_id'), 'traffic_logs', ['spot_id'], unique=False)
    op.create_index(op.f('ix_traffic_logs_order_id'), 'traffic_logs', ['order_id'], unique=False)
    op.create_index(op.f('ix_traffic_logs_campaign_id'), 'traffic_logs', ['campaign_id'], unique=False)
    op.create_index(op.f('ix_traffic_logs_copy_id'), 'traffic_logs', ['copy_id'], unique=False)
    op.create_index(op.f('ix_traffic_logs_user_id'), 'traffic_logs', ['user_id'], unique=False)
    op.create_index(op.f('ix_traffic_logs_created_at'), 'traffic_logs', ['created_at'], unique=False)
    
    # Create order_workflow_states table with UUID primary key and UUID foreign keys
    op.create_table(
        'order_workflow_states',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('order_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('orders.id', ondelete='CASCADE'), nullable=False),
        sa.Column('state', postgresql.ENUM('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'TRAFFIC_READY', 'BILLING_READY', 'LOCKED', 'CANCELLED', name='workflowstate', create_type=False), nullable=False),
        sa.Column('notes', sa.Text(), nullable=True),
        sa.Column('state_changed_by', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('state_changed_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_order_workflow_states_id'), 'order_workflow_states', ['id'], unique=False)
    op.create_index(op.f('ix_order_workflow_states_order_id'), 'order_workflow_states', ['order_id'], unique=False)
    op.create_index(op.f('ix_order_workflow_states_state'), 'order_workflow_states', ['state'], unique=False)
    op.create_index(op.f('ix_order_workflow_states_state_changed_at'), 'order_workflow_states', ['state_changed_at'], unique=False)
    
    # Create order_templates table with UUID primary key and UUID foreign keys
    op.create_table(
        'order_templates',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('name', sa.String(length=255), nullable=False),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('default_spot_lengths', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('default_rate_type', sa.String(length=20), nullable=True),
        sa.Column('default_rates', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('created_by', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_order_templates_id'), 'order_templates', ['id'], unique=False)
    op.create_index(op.f('ix_order_templates_name'), 'order_templates', ['name'], unique=False)
    
    # Create production_comments table with UUID primary key and UUID foreign keys
    op.create_table(
        'production_comments',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('production_order_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('production_orders.id', ondelete='CASCADE'), nullable=False),
        sa.Column('comment_text', sa.Text(), nullable=False),
        sa.Column('author_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('parent_comment_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('production_comments.id', ondelete='CASCADE'), nullable=True),
        sa.Column('attachments', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('is_internal', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_production_comments_id'), 'production_comments', ['id'], unique=False)
    op.create_index(op.f('ix_production_comments_production_order_id'), 'production_comments', ['production_order_id'], unique=False)
    op.create_index(op.f('ix_production_comments_author_id'), 'production_comments', ['author_id'], unique=False)
    
    # Create production_revisions table with UUID primary key and UUID foreign keys
    op.create_table(
        'production_revisions',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('production_order_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('production_orders.id', ondelete='CASCADE'), nullable=False),
        sa.Column('revision_number', sa.Integer(), nullable=False),
        sa.Column('requested_by', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('reason', sa.Text(), nullable=False),
        sa.Column('notes', sa.Text(), nullable=True),
        sa.Column('previous_revision_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('production_revisions.id', ondelete='SET NULL'), nullable=True),
        sa.Column('requested_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('completed_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_production_revisions_id'), 'production_revisions', ['id'], unique=False)
    op.create_index(op.f('ix_production_revisions_production_order_id'), 'production_revisions', ['production_order_id'], unique=False)
    
    # Create voice_talent_requests table with UUID primary key and UUID foreign keys
    op.create_table(
        'voice_talent_requests',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('production_order_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('production_orders.id', ondelete='CASCADE'), nullable=False),
        sa.Column('talent_user_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='SET NULL'), nullable=True),
        sa.Column('talent_type', postgresql.ENUM('male', 'female', 'character', 'ae_voice', 'any', name='talenttype', create_type=False), nullable=False),
        sa.Column('script', sa.Text(), nullable=False),
        sa.Column('pronunciation_guides', sa.Text(), nullable=True),
        sa.Column('talent_instructions', sa.Text(), nullable=True),
        sa.Column('status', postgresql.ENUM('PENDING', 'ASSIGNED', 'RECORDING', 'UPLOADED', 'APPROVED', 'REJECTED', name='talentrequeststatus', create_type=False), nullable=False, server_default='PENDING'),
        sa.Column('takes', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('assigned_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('deadline', sa.DateTime(timezone=True), nullable=True),
        sa.Column('completed_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_voice_talent_requests_id'), 'voice_talent_requests', ['id'], unique=False)
    op.create_index(op.f('ix_voice_talent_requests_production_order_id'), 'voice_talent_requests', ['production_order_id'], unique=False)
    op.create_index(op.f('ix_voice_talent_requests_talent_user_id'), 'voice_talent_requests', ['talent_user_id'], unique=False)
    op.create_index(op.f('ix_voice_talent_requests_status'), 'voice_talent_requests', ['status'], unique=False)
    op.create_index(op.f('ix_voice_talent_requests_deadline'), 'voice_talent_requests', ['deadline'], unique=False)
    
    # Create payments table with UUID primary key and UUID foreign keys
    op.create_table(
        'payments',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('invoice_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('invoices.id', ondelete='CASCADE'), nullable=False),
        sa.Column('amount', sa.Numeric(10, 2), nullable=False),
        sa.Column('payment_date', sa.DateTime(timezone=True), nullable=False),
        sa.Column('payment_method', sa.String(length=50), nullable=True),
        sa.Column('reference_number', sa.String(length=100), nullable=True),
        sa.Column('notes', sa.String(length=500), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_payments_id'), 'payments', ['id'], unique=False)
    op.create_index(op.f('ix_payments_invoice_id'), 'payments', ['invoice_id'], unique=False)
    op.create_index(op.f('ix_payments_payment_date'), 'payments', ['payment_date'], unique=False)
    
    # Create makegoods table with UUID primary key and UUID foreign keys
    op.create_table(
        'makegoods',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('original_spot_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('spots.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('makegood_spot_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('spots.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('campaign_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('campaigns.id', ondelete='SET NULL'), nullable=True),
        sa.Column('reason', sa.String(length=500), nullable=True),
        sa.Column('approved_by', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='SET NULL'), nullable=True),
        sa.Column('approved_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_makegoods_id'), 'makegoods', ['id'], unique=False)
    op.create_index(op.f('ix_makegoods_original_spot_id'), 'makegoods', ['original_spot_id'], unique=False)
    op.create_index(op.f('ix_makegoods_makegood_spot_id'), 'makegoods', ['makegood_spot_id'], unique=False)
    op.create_index(op.f('ix_makegoods_campaign_id'), 'makegoods', ['campaign_id'], unique=False)
    
    # Create voice_track_slots table with UUID primary key and UUID foreign keys
    op.create_table(
        'voice_track_slots',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('log_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('daily_logs.id', ondelete='CASCADE'), nullable=False),
        sa.Column('hour', sa.Integer(), nullable=False),
        sa.Column('break_position', sa.String(length=10), nullable=True),
        sa.Column('standardized_name', sa.String(length=50), nullable=True),
        sa.Column('assigned_dj_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='SET NULL'), nullable=True),
        sa.Column('voice_track_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('voice_tracks.id', ondelete='SET NULL'), nullable=True),
        sa.Column('previous_track_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('tracks.id', ondelete='SET NULL'), nullable=True),
        sa.Column('next_track_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('tracks.id', ondelete='SET NULL'), nullable=True),
        sa.Column('ramp_time', sa.Numeric(5, 2), nullable=True),
        sa.Column('status', sa.String(length=20), nullable=False, server_default='pending'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_voice_track_slots_id'), 'voice_track_slots', ['id'], unique=False)
    op.create_index(op.f('ix_voice_track_slots_log_id'), 'voice_track_slots', ['log_id'], unique=False)
    op.create_index(op.f('ix_voice_track_slots_hour'), 'voice_track_slots', ['hour'], unique=False)
    op.create_index(op.f('ix_voice_track_slots_standardized_name'), 'voice_track_slots', ['standardized_name'], unique=False)
    op.create_index(op.f('ix_voice_track_slots_assigned_dj_id'), 'voice_track_slots', ['assigned_dj_id'], unique=False)
    op.create_index(op.f('ix_voice_track_slots_voice_track_id'), 'voice_track_slots', ['voice_track_id'], unique=False)
    op.create_index(op.f('ix_voice_track_slots_status'), 'voice_track_slots', ['status'], unique=False)
    
    # Create voice_track_audit table with UUID primary key and UUID foreign keys
    op.create_table(
        'voice_track_audit',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('voice_track_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('voice_tracks.id', ondelete='CASCADE'), nullable=False),
        sa.Column('action', sa.String(length=50), nullable=False),
        sa.Column('user_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('timestamp', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('audit_metadata', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
    )
    op.create_index(op.f('ix_voice_track_audit_id'), 'voice_track_audit', ['id'], unique=False)
    op.create_index(op.f('ix_voice_track_audit_voice_track_id'), 'voice_track_audit', ['voice_track_id'], unique=False)
    op.create_index(op.f('ix_voice_track_audit_user_id'), 'voice_track_audit', ['user_id'], unique=False)
    op.create_index(op.f('ix_voice_track_audit_timestamp'), 'voice_track_audit', ['timestamp'], unique=False)
    
    # Create log_revisions table with UUID primary key and UUID foreign keys
    op.create_table(
        'log_revisions',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('log_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('daily_logs.id', ondelete='CASCADE'), nullable=False),
        sa.Column('revision_number', sa.Integer(), nullable=False),
        sa.Column('json_data', postgresql.JSONB(astext_type=sa.Text()), nullable=False),
        sa.Column('changed_by', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('change_summary', sa.Text(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_log_revisions_id'), 'log_revisions', ['id'], unique=False)
    op.create_index(op.f('ix_log_revisions_log_id'), 'log_revisions', ['log_id'], unique=False)
    op.create_index(op.f('ix_log_revisions_revision_number'), 'log_revisions', ['revision_number'], unique=False)
    op.create_index(op.f('ix_log_revisions_created_at'), 'log_revisions', ['created_at'], unique=False)
    
    # Create daypart_categories table with UUID primary key
    op.create_table(
        'daypart_categories',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('name', sa.String(length=100), nullable=False, unique=True),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('color', sa.String(length=7), nullable=True),
        sa.Column('icon', sa.String(length=50), nullable=True),
        sa.Column('sort_order', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_daypart_categories_id'), 'daypart_categories', ['id'], unique=False)
    op.create_index(op.f('ix_daypart_categories_name'), 'daypart_categories', ['name'], unique=True)
    op.create_index(op.f('ix_daypart_categories_sort_order'), 'daypart_categories', ['sort_order'], unique=False)
    op.create_index(op.f('ix_daypart_categories_active'), 'daypart_categories', ['active'], unique=False)
    
    # Create dayparts table with UUID primary key and UUID foreign keys
    op.create_table(
        'dayparts',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('name', sa.String(length=100), nullable=False),
        sa.Column('start_time', sa.Time(), nullable=False),
        sa.Column('end_time', sa.Time(), nullable=False),
        sa.Column('days_of_week', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('category_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('daypart_categories.id', ondelete='SET NULL'), nullable=True),
        sa.Column('station_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('stations.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('description', sa.String(length=255), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_dayparts_id'), 'dayparts', ['id'], unique=False)
    op.create_index(op.f('ix_dayparts_name'), 'dayparts', ['name'], unique=False)
    op.create_index(op.f('ix_dayparts_category_id'), 'dayparts', ['category_id'], unique=False)
    op.create_index(op.f('ix_dayparts_station_id'), 'dayparts', ['station_id'], unique=False)
    op.create_index(op.f('ix_dayparts_active'), 'dayparts', ['active'], unique=False)
    
    # Create clock_templates table with UUID primary key and UUID foreign keys
    op.create_table(
        'clock_templates',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('name', sa.String(length=100), nullable=False),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('json_layout', postgresql.JSONB(astext_type=sa.Text()), nullable=False),
        sa.Column('station_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('stations.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_clock_templates_id'), 'clock_templates', ['id'], unique=False)
    op.create_index(op.f('ix_clock_templates_name'), 'clock_templates', ['name'], unique=False)
    op.create_index(op.f('ix_clock_templates_station_id'), 'clock_templates', ['station_id'], unique=False)
    
    # Create break_structures table with UUID primary key and UUID foreign keys
    op.create_table(
        'break_structures',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('name', sa.String(length=100), nullable=False),
        sa.Column('hour', sa.Integer(), nullable=False),
        sa.Column('break_positions', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('station_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('stations.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.CheckConstraint('hour >= 0 AND hour <= 23', name='break_structures_hour_check'),
    )
    op.create_index(op.f('ix_break_structures_id'), 'break_structures', ['id'], unique=False)
    op.create_index(op.f('ix_break_structures_name'), 'break_structures', ['name'], unique=False)
    op.create_index(op.f('ix_break_structures_hour'), 'break_structures', ['hour'], unique=False)
    op.create_index(op.f('ix_break_structures_station_id'), 'break_structures', ['station_id'], unique=False)
    op.create_index(op.f('ix_break_structures_active'), 'break_structures', ['active'], unique=False)
    
    # Create rotation_rules table with UUID primary key and UUID foreign keys
    op.create_table(
        'rotation_rules',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('name', sa.String(length=100), nullable=False),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('rotation_type', sa.String(length=20), nullable=False, server_default='SEQUENTIAL'),
        sa.Column('daypart_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('dayparts.id', ondelete='SET NULL'), nullable=True),
        sa.Column('campaign_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('campaigns.id', ondelete='SET NULL'), nullable=True),
        sa.Column('min_separation', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('max_per_hour', sa.Integer(), nullable=True),
        sa.Column('max_per_day', sa.Integer(), nullable=True),
        sa.Column('weights', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('cut_specific', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('cut_weights', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('program_specific', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('exclude_days', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('exclude_times', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('priority', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_rotation_rules_id'), 'rotation_rules', ['id'], unique=False)
    op.create_index(op.f('ix_rotation_rules_name'), 'rotation_rules', ['name'], unique=False)
    op.create_index(op.f('ix_rotation_rules_daypart_id'), 'rotation_rules', ['daypart_id'], unique=False)
    op.create_index(op.f('ix_rotation_rules_campaign_id'), 'rotation_rules', ['campaign_id'], unique=False)
    op.create_index(op.f('ix_rotation_rules_active'), 'rotation_rules', ['active'], unique=False)
    
    # Create inventory_slots table with UUID primary key
    op.create_table(
        'inventory_slots',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('date', sa.Date(), nullable=False),
        sa.Column('hour', sa.Integer(), nullable=False),
        sa.Column('break_position', sa.String(length=10), nullable=True),
        sa.Column('daypart', sa.String(length=50), nullable=True),
        sa.Column('available', sa.Integer(), nullable=False, server_default='3600'),
        sa.Column('booked', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('sold_out', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('revenue', sa.Numeric(10, 2), nullable=False, server_default='0'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.CheckConstraint('hour >= 0 AND hour <= 23', name='inventory_slots_hour_check'),
    )
    op.create_index(op.f('ix_inventory_slots_id'), 'inventory_slots', ['id'], unique=False)
    op.create_index(op.f('ix_inventory_slots_date'), 'inventory_slots', ['date'], unique=False)
    op.create_index(op.f('ix_inventory_slots_hour'), 'inventory_slots', ['hour'], unique=False)
    op.create_index(op.f('ix_inventory_slots_sold_out'), 'inventory_slots', ['sold_out'], unique=False)
    
    # Create audio_cuts table with UUID primary key and UUID foreign keys
    op.create_table(
        'audio_cuts',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('copy_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('copy.id', ondelete='CASCADE'), nullable=False),
        sa.Column('cut_id', sa.String(length=50), nullable=False),
        sa.Column('cut_name', sa.String(length=255), nullable=True),
        sa.Column('audio_file_path', sa.Text(), nullable=True),
        sa.Column('audio_file_url', sa.Text(), nullable=True),
        sa.Column('file_checksum', sa.String(length=64), nullable=True),
        sa.Column('version', sa.Integer(), nullable=False, server_default='1'),
        sa.Column('rotation_weight', sa.Float(), nullable=False, server_default='1.0'),
        sa.Column('daypart_restrictions', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('program_associations', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('expires_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('qc_results', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('notes', sa.Text(), nullable=True),
        sa.Column('tags', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('created_by', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='SET NULL'), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.CheckConstraint('rotation_weight >= 0', name='check_rotation_weight_positive'),
    )
    op.create_index(op.f('ix_audio_cuts_id'), 'audio_cuts', ['id'], unique=False)
    op.create_index(op.f('ix_audio_cuts_copy_id'), 'audio_cuts', ['copy_id'], unique=False)
    op.create_index(op.f('ix_audio_cuts_cut_id'), 'audio_cuts', ['cut_id'], unique=False)
    op.create_index(op.f('ix_audio_cuts_expires_at'), 'audio_cuts', ['expires_at'], unique=False)
    op.create_index(op.f('ix_audio_cuts_active'), 'audio_cuts', ['active'], unique=False)
    
    # Create audio_versions table with UUID primary key and UUID foreign keys
    op.create_table(
        'audio_versions',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('cut_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('audio_cuts.id', ondelete='CASCADE'), nullable=False),
        sa.Column('version_number', sa.Integer(), nullable=False),
        sa.Column('audio_file_path', sa.Text(), nullable=True),
        sa.Column('audio_file_url', sa.Text(), nullable=True),
        sa.Column('file_checksum', sa.String(length=64), nullable=True),
        sa.Column('version_notes', sa.Text(), nullable=True),
        sa.Column('changed_by', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='SET NULL'), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_audio_versions_id'), 'audio_versions', ['id'], unique=False)
    op.create_index(op.f('ix_audio_versions_cut_id'), 'audio_versions', ['cut_id'], unique=False)
    
    # Create audio_deliveries table with UUID primary key and UUID foreign keys
    op.create_table(
        'audio_deliveries',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('cut_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('audio_cuts.id', ondelete='CASCADE'), nullable=False),
        sa.Column('copy_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('copy.id', ondelete='SET NULL'), nullable=True),
        sa.Column('delivery_method', sa.String(length=20), nullable=False),
        sa.Column('target_server', sa.String(length=255), nullable=False),
        sa.Column('target_path', sa.Text(), nullable=True),
        sa.Column('status', sa.String(length=20), nullable=False, server_default='pending'),
        sa.Column('retry_count', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('max_retries', sa.Integer(), nullable=False, server_default='3'),
        sa.Column('source_checksum', sa.String(length=64), nullable=True),
        sa.Column('delivered_checksum', sa.String(length=64), nullable=True),
        sa.Column('checksum_verified', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('delivery_started_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('delivery_completed_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('last_retry_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('error_message', sa.Text(), nullable=True),
        sa.Column('error_details', sa.Text(), nullable=True),
    )
    op.create_index(op.f('ix_audio_deliveries_id'), 'audio_deliveries', ['id'], unique=False)
    op.create_index(op.f('ix_audio_deliveries_cut_id'), 'audio_deliveries', ['cut_id'], unique=False)
    op.create_index(op.f('ix_audio_deliveries_copy_id'), 'audio_deliveries', ['copy_id'], unique=False)
    op.create_index(op.f('ix_audio_deliveries_status'), 'audio_deliveries', ['status'], unique=False)
    
    # Create audio_qc_results table with UUID primary key and UUID foreign keys
    op.create_table(
        'audio_qc_results',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('cut_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('audio_cuts.id', ondelete='CASCADE'), nullable=False),
        sa.Column('version_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('audio_versions.id', ondelete='SET NULL'), nullable=True),
        sa.Column('qc_data', postgresql.JSONB(astext_type=sa.Text()), nullable=False),
        sa.Column('duration_seconds', sa.Float(), nullable=True),
        sa.Column('sample_rate', sa.Integer(), nullable=True),
        sa.Column('bitrate', sa.Integer(), nullable=True),
        sa.Column('channels', sa.Integer(), nullable=True),
        sa.Column('silence_at_head', sa.Float(), nullable=True),
        sa.Column('silence_at_tail', sa.Float(), nullable=True),
        sa.Column('silence_detected', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('peak_level', sa.Float(), nullable=True),
        sa.Column('rms_level', sa.Float(), nullable=True),
        sa.Column('lufs_level', sa.Float(), nullable=True),
        sa.Column('normalization_applied', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('normalization_method', sa.String(length=50), nullable=True),
        sa.Column('clipping_detected', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('clipping_samples', sa.Integer(), nullable=True),
        sa.Column('volume_threshold_passed', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('minimum_volume', sa.Float(), nullable=True),
        sa.Column('format_valid', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('format_type', sa.String(length=20), nullable=True),
        sa.Column('format_errors', sa.Text(), nullable=True),
        sa.Column('file_corrupted', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('corruption_details', sa.Text(), nullable=True),
        sa.Column('qc_passed', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('qc_warnings', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('qc_errors', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('overridden', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('overridden_by', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='SET NULL'), nullable=True),
        sa.Column('override_reason', sa.Text(), nullable=True),
        sa.Column('override_timestamp', sa.DateTime(timezone=True), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_audio_qc_results_id'), 'audio_qc_results', ['id'], unique=False)
    op.create_index(op.f('ix_audio_qc_results_cut_id'), 'audio_qc_results', ['cut_id'], unique=False)
    op.create_index(op.f('ix_audio_qc_results_version_id'), 'audio_qc_results', ['version_id'], unique=False)
    op.create_index(op.f('ix_audio_qc_results_silence_detected'), 'audio_qc_results', ['silence_detected'], unique=False)
    op.create_index(op.f('ix_audio_qc_results_clipping_detected'), 'audio_qc_results', ['clipping_detected'], unique=False)
    op.create_index(op.f('ix_audio_qc_results_format_valid'), 'audio_qc_results', ['format_valid'], unique=False)
    op.create_index(op.f('ix_audio_qc_results_file_corrupted'), 'audio_qc_results', ['file_corrupted'], unique=False)
    op.create_index(op.f('ix_audio_qc_results_qc_passed'), 'audio_qc_results', ['qc_passed'], unique=False)
    
    # Create live_reads table with UUID primary key and UUID foreign keys
    op.create_table(
        'live_reads',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('copy_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('copy.id', ondelete='SET NULL'), nullable=True),
        sa.Column('order_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('orders.id', ondelete='SET NULL'), nullable=True),
        sa.Column('advertiser_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('advertisers.id', ondelete='SET NULL'), nullable=True),
        sa.Column('script_text', sa.Text(), nullable=False),
        sa.Column('script_title', sa.String(length=255), nullable=True),
        sa.Column('scheduled_time', sa.DateTime(timezone=True), nullable=True),
        sa.Column('scheduled_date', sa.DateTime(timezone=True), nullable=True),
        sa.Column('performed_time', sa.DateTime(timezone=True), nullable=True),
        sa.Column('performed_by', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='SET NULL'), nullable=True),
        sa.Column('confirmed', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('proof_of_performance', sa.Text(), nullable=True),
        sa.Column('confirmation_timestamp', sa.DateTime(timezone=True), nullable=True),
        sa.Column('status', sa.String(length=50), nullable=False, server_default='scheduled'),
        sa.Column('makegood_required', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('makegood_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('makegoods.id', ondelete='SET NULL'), nullable=True),
        sa.Column('notes', sa.Text(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_live_reads_id'), 'live_reads', ['id'], unique=False)
    op.create_index(op.f('ix_live_reads_copy_id'), 'live_reads', ['copy_id'], unique=False)
    op.create_index(op.f('ix_live_reads_order_id'), 'live_reads', ['order_id'], unique=False)
    op.create_index(op.f('ix_live_reads_advertiser_id'), 'live_reads', ['advertiser_id'], unique=False)
    op.create_index(op.f('ix_live_reads_scheduled_time'), 'live_reads', ['scheduled_time'], unique=False)
    op.create_index(op.f('ix_live_reads_scheduled_date'), 'live_reads', ['scheduled_date'], unique=False)
    op.create_index(op.f('ix_live_reads_performed_time'), 'live_reads', ['performed_time'], unique=False)
    op.create_index(op.f('ix_live_reads_confirmed'), 'live_reads', ['confirmed'], unique=False)
    op.create_index(op.f('ix_live_reads_status'), 'live_reads', ['status'], unique=False)
    op.create_index(op.f('ix_live_reads_makegood_required'), 'live_reads', ['makegood_required'], unique=False)
    
    # Create political_records table with UUID primary key and UUID foreign keys
    op.create_table(
        'political_records',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('copy_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('copy.id', ondelete='SET NULL'), nullable=True),
        sa.Column('order_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('orders.id', ondelete='SET NULL'), nullable=True),
        sa.Column('advertiser_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('advertisers.id', ondelete='SET NULL'), nullable=True),
        sa.Column('advertiser_category', sa.String(length=100), nullable=False),
        sa.Column('sponsor_name', sa.String(length=255), nullable=False),
        sa.Column('sponsor_id', sa.String(length=100), nullable=True),
        sa.Column('office_sought', sa.String(length=255), nullable=True),
        sa.Column('disclaimers_required', sa.Text(), nullable=True),
        sa.Column('disclaimers_included', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('compliance_status', sa.String(length=50), nullable=False, server_default='pending'),
        sa.Column('no_substitution_allowed', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('archived', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('archive_date', sa.DateTime(timezone=True), nullable=True),
        sa.Column('archive_location', sa.Text(), nullable=True),
        sa.Column('political_window_start', sa.DateTime(timezone=True), nullable=True),
        sa.Column('political_window_end', sa.DateTime(timezone=True), nullable=True),
        sa.Column('no_preemptions', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('priority_scheduling', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('election_cycle', sa.String(length=100), nullable=True),
        sa.Column('lowest_unit_rate_record', sa.Text(), nullable=True),
        sa.Column('class_comparison', sa.Text(), nullable=True),
        sa.Column('window_dates', sa.Text(), nullable=True),
        sa.Column('substantiation_documentation', sa.Text(), nullable=True),
        sa.Column('notes', sa.Text(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_political_records_id'), 'political_records', ['id'], unique=False)
    op.create_index(op.f('ix_political_records_copy_id'), 'political_records', ['copy_id'], unique=False)
    op.create_index(op.f('ix_political_records_order_id'), 'political_records', ['order_id'], unique=False)
    op.create_index(op.f('ix_political_records_advertiser_id'), 'political_records', ['advertiser_id'], unique=False)
    op.create_index(op.f('ix_political_records_sponsor_name'), 'political_records', ['sponsor_name'], unique=False)
    op.create_index(op.f('ix_political_records_compliance_status'), 'political_records', ['compliance_status'], unique=False)
    op.create_index(op.f('ix_political_records_archived'), 'political_records', ['archived'], unique=False)
    op.create_index(op.f('ix_political_records_election_cycle'), 'political_records', ['election_cycle'], unique=False)
    
    # Create notifications table with UUID primary key and UUID foreign keys
    op.create_table(
        'notifications',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('user_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='CASCADE'), nullable=False),
        sa.Column('notification_type', postgresql.ENUM('EMAIL', 'IN_APP', 'BOTH', name='notificationtype', create_type=False), nullable=False, server_default='IN_APP'),
        sa.Column('status', postgresql.ENUM('PENDING', 'SENT', 'FAILED', 'READ', name='notificationstatus', create_type=False), nullable=False, server_default='PENDING'),
        sa.Column('subject', sa.String(length=255), nullable=True),
        sa.Column('message', sa.Text(), nullable=False),
        sa.Column('meta_data', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('read_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('sent_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('error_message', sa.Text(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_notifications_id'), 'notifications', ['id'], unique=False)
    op.create_index(op.f('ix_notifications_user_id'), 'notifications', ['user_id'], unique=False)
    op.create_index(op.f('ix_notifications_status'), 'notifications', ['status'], unique=False)
    op.create_index(op.f('ix_notifications_created_at'), 'notifications', ['created_at'], unique=False)
    
    # Create audit_logs table with UUID primary key and UUID foreign keys
    op.create_table(
        'audit_logs',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('user_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('action', sa.String(length=100), nullable=False),
        sa.Column('resource_type', sa.String(length=50), nullable=False),
        sa.Column('resource_id', postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column('details', sa.String(), nullable=True),
        sa.Column('ip_address', sa.String(length=45), nullable=True),
        sa.Column('user_agent', sa.String(length=500), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_audit_logs_id'), 'audit_logs', ['id'], unique=False)
    op.create_index(op.f('ix_audit_logs_user_id'), 'audit_logs', ['user_id'], unique=False)
    op.create_index(op.f('ix_audit_logs_action'), 'audit_logs', ['action'], unique=False)
    op.create_index(op.f('ix_audit_logs_resource_type'), 'audit_logs', ['resource_type'], unique=False)
    op.create_index(op.f('ix_audit_logs_resource_id'), 'audit_logs', ['resource_id'], unique=False)
    op.create_index(op.f('ix_audit_logs_created_at'), 'audit_logs', ['created_at'], unique=False)
    
    # Create webhooks table with UUID primary key and UUID foreign keys
    op.create_table(
        'webhooks',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('name', sa.String(length=255), nullable=False),
        sa.Column('webhook_type', postgresql.ENUM('DISCORD', 'SLACK', 'CUSTOM', name='webhooktype', create_type=False), nullable=False),
        sa.Column('url', sa.Text(), nullable=False),
        sa.Column('events', postgresql.JSONB(astext_type=sa.Text()), nullable=False),
        sa.Column('secret', sa.String(length=255), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('last_triggered_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('last_error', sa.Text(), nullable=True),
        sa.Column('error_count', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('headers', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('created_by', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_webhooks_id'), 'webhooks', ['id'], unique=False)
    op.create_index(op.f('ix_webhooks_webhook_type'), 'webhooks', ['webhook_type'], unique=False)
    op.create_index(op.f('ix_webhooks_active'), 'webhooks', ['active'], unique=False)
    
    # Create backups table with UUID primary key and UUID foreign keys
    op.create_table(
        'backups',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('backup_type', postgresql.ENUM('FULL', 'DATABASE', 'FILES', name='backuptype', create_type=False), nullable=False, server_default='FULL'),
        sa.Column('status', postgresql.ENUM('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', name='backupstatus', create_type=False), nullable=False, server_default='PENDING'),
        sa.Column('storage_provider', postgresql.ENUM('LOCAL', 'S3', 'BACKBLAZE_B2', name='storageprovider', create_type=False), nullable=False, server_default='LOCAL'),
        sa.Column('filename', sa.String(length=255), nullable=False),
        sa.Column('file_path', sa.Text(), nullable=True),
        sa.Column('remote_path', sa.Text(), nullable=True),
        sa.Column('file_size', sa.Float(), nullable=True),
        sa.Column('database_dump', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('files_included', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('started_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('completed_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('error_message', sa.Text(), nullable=True),
        sa.Column('created_by', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='RESTRICT'), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_backups_id'), 'backups', ['id'], unique=False)
    op.create_index(op.f('ix_backups_status'), 'backups', ['status'], unique=False)
    op.create_index(op.f('ix_backups_created_at'), 'backups', ['created_at'], unique=False)
    
    # Create playback_history table with UUID primary key and UUID foreign keys
    op.create_table(
        'playback_history',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('track_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('tracks.id', ondelete='CASCADE'), nullable=False),
        sa.Column('log_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('daily_logs.id', ondelete='CASCADE'), nullable=False),
        sa.Column('played_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('duration_played', sa.Integer(), nullable=True),
    )
    op.create_index(op.f('ix_playback_history_id'), 'playback_history', ['id'], unique=False)
    op.create_index(op.f('ix_playback_history_track_id'), 'playback_history', ['track_id'], unique=False)
    op.create_index(op.f('ix_playback_history_log_id'), 'playback_history', ['log_id'], unique=False)
    op.create_index(op.f('ix_playback_history_played_at'), 'playback_history', ['played_at'], unique=False)
    
    # Create digital_orders table with UUID primary key and UUID foreign keys
    op.create_table(
        'digital_orders',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('order_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('orders.id', ondelete='CASCADE'), nullable=False),
        sa.Column('platform', postgresql.ENUM('WEBSITE', 'PODCAST', 'STREAMING', 'SOCIAL_MEDIA', name='platform', create_type=False), nullable=False),
        sa.Column('impression_target', sa.Integer(), nullable=True),
        sa.Column('cpm', sa.Numeric(10, 2), nullable=True),
        sa.Column('cpc', sa.Numeric(10, 2), nullable=True),
        sa.Column('flat_rate', sa.Numeric(10, 2), nullable=True),
        sa.Column('creative_assets', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('delivered_impressions', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('status', postgresql.ENUM('DRAFT', 'ACTIVE', 'COMPLETED', 'CANCELLED', name='digitalorderstatus', create_type=False), nullable=False, server_default='DRAFT'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_digital_orders_id'), 'digital_orders', ['id'], unique=False)
    op.create_index(op.f('ix_digital_orders_order_id'), 'digital_orders', ['order_id'], unique=False)
    op.create_index(op.f('ix_digital_orders_status'), 'digital_orders', ['status'], unique=False)
    
    # Create sales_goals table with UUID primary key and UUID foreign keys
    op.create_table(
        'sales_goals',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('sales_rep_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('sales_reps.id', ondelete='CASCADE'), nullable=False),
        sa.Column('period', postgresql.ENUM('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY', name='periodtype', create_type=False), nullable=False),
        sa.Column('target_date', sa.Date(), nullable=False),
        sa.Column('goal_amount', sa.Numeric(10, 2), nullable=False),
        sa.Column('actual_amount', sa.Numeric(10, 2), nullable=False, server_default='0'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_sales_goals_id'), 'sales_goals', ['id'], unique=False)
    op.create_index(op.f('ix_sales_goals_sales_rep_id'), 'sales_goals', ['sales_rep_id'], unique=False)
    op.create_index(op.f('ix_sales_goals_period'), 'sales_goals', ['period'], unique=False)
    op.create_index(op.f('ix_sales_goals_target_date'), 'sales_goals', ['target_date'], unique=False)


def downgrade():
    """
    Drop all tables (reverse of upgrade).
    This will remove all UUID-based tables.
    """
    # Drop all tables (CASCADE handles foreign key dependencies)
    op.execute("""
        DO $$ DECLARE
            r RECORD;
        BEGIN
            FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public') LOOP
                EXECUTE 'DROP TABLE IF EXISTS ' || quote_ident(r.tablename) || ' CASCADE';
            END LOOP;
        END $$;
    """)
    
    # Drop all enum types
    op.execute("""
        DO $$ DECLARE
            r RECORD;
        BEGIN
            FOR r IN (SELECT typname FROM pg_type WHERE typtype = 'e' AND typnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public')) LOOP
                EXECUTE 'DROP TYPE IF EXISTS ' || quote_ident(r.typname) || ' CASCADE';
            END LOOP;
        END $$;
    """)

