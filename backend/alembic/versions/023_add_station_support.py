"""Add multi-station support across all modules

Revision ID: 023
Revises: 022
Create Date: 2025-01-20

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql
from sqlalchemy import text

# revision identifiers, used by Alembic.
revision = '023'
down_revision = '022'
branch_labels = None
depends_on = None


def upgrade():
    # Check if tables already exist
    from sqlalchemy import inspect
    conn = op.get_bind()
    inspector = inspect(conn)
    existing_tables = inspector.get_table_names()
    
    # Step 1: Create user_stations association table
    if 'user_stations' not in existing_tables:
        op.create_table(
            'user_stations',
            sa.Column('user_id', sa.Integer(), nullable=False),
            sa.Column('station_id', sa.Integer(), nullable=False),
            sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
            sa.ForeignKeyConstraint(['station_id'], ['stations.id'], ondelete='CASCADE'),
            sa.PrimaryKeyConstraint('user_id', 'station_id')
        )
        op.create_index('ix_user_stations_user_id', 'user_stations', ['user_id'])
        op.create_index('ix_user_stations_station_id', 'user_stations', ['station_id'])
    
    # Step 2: Create default station if none exists
    conn.execute(text("""
        INSERT INTO stations (call_letters, frequency, market, format, active, created_at, updated_at)
        SELECT 'DEFAULT', 'N/A', 'Default Market', 'Default Format', true, NOW(), NOW()
        WHERE NOT EXISTS (SELECT 1 FROM stations LIMIT 1)
    """))
    
    # Get default station ID
    result = conn.execute(text("SELECT id FROM stations WHERE call_letters = 'DEFAULT' LIMIT 1"))
    default_station_id = result.scalar()
    
    if not default_station_id:
        # If DEFAULT station wasn't created, get the first station
        result = conn.execute(text("SELECT id FROM stations ORDER BY id LIMIT 1"))
        default_station_id = result.scalar()
    
    if not default_station_id:
        raise Exception("No stations exist and could not create default station")
    
    # Step 3: Add station_id columns (nullable initially)
    # Spots
    if 'spots' in existing_tables:
        existing_columns = [col['name'] for col in inspector.get_columns('spots')]
        if 'station_id' not in existing_columns:
            op.add_column('spots', sa.Column('station_id', sa.Integer(), nullable=True))
            op.create_foreign_key('fk_spots_station_id', 'spots', 'stations', ['station_id'], ['id'])
            op.create_index('ix_spots_station_id', 'spots', ['station_id'])
            # Migrate existing data
            conn.execute(text(f"UPDATE spots SET station_id = {default_station_id} WHERE station_id IS NULL"))
            # Make NOT NULL
            op.alter_column('spots', 'station_id', nullable=False)
    
    # Daily logs
    if 'daily_logs' in existing_tables:
        existing_columns = [col['name'] for col in inspector.get_columns('daily_logs')]
        if 'station_id' not in existing_columns:
            op.add_column('daily_logs', sa.Column('station_id', sa.Integer(), nullable=True))
            op.create_foreign_key('fk_daily_logs_station_id', 'daily_logs', 'stations', ['station_id'], ['id'])
            op.create_index('ix_daily_logs_station_id', 'daily_logs', ['station_id'])
            # Migrate existing data
            conn.execute(text(f"UPDATE daily_logs SET station_id = {default_station_id} WHERE station_id IS NULL"))
            # Make NOT NULL
            op.alter_column('daily_logs', 'station_id', nullable=False)
            # Add unique constraint (date, station_id)
            try:
                op.create_unique_constraint('uq_daily_logs_date_station', 'daily_logs', ['date', 'station_id'])
            except Exception:
                pass  # Constraint might already exist
    
    # Order lines - replace station string with station_id FK
    if 'order_lines' in existing_tables:
        existing_columns = [col['name'] for col in inspector.get_columns('order_lines')]
        if 'station_id' not in existing_columns:
            op.add_column('order_lines', sa.Column('station_id', sa.Integer(), nullable=True))
            op.create_foreign_key('fk_order_lines_station_id', 'order_lines', 'stations', ['station_id'], ['id'])
            op.create_index('ix_order_lines_station_id', 'order_lines', ['station_id'])
            # Migrate existing data - try to match station names to station call_letters
            # For now, assign default station
            conn.execute(text(f"UPDATE order_lines SET station_id = {default_station_id} WHERE station_id IS NULL"))
            # Make NOT NULL
            op.alter_column('order_lines', 'station_id', nullable=False)
            # Drop old station string column (keep for now in case of issues, can drop later)
            # op.drop_column('order_lines', 'station')
    
    # Invoice lines
    if 'invoice_lines' in existing_tables:
        existing_columns = [col['name'] for col in inspector.get_columns('invoice_lines')]
        if 'station_id' not in existing_columns:
            op.add_column('invoice_lines', sa.Column('station_id', sa.Integer(), nullable=True))
            op.create_foreign_key('fk_invoice_lines_station_id', 'invoice_lines', 'stations', ['station_id'], ['id'])
            op.create_index('ix_invoice_lines_station_id', 'invoice_lines', ['station_id'])
            # Migrate existing data
            conn.execute(text(f"UPDATE invoice_lines SET station_id = {default_station_id} WHERE station_id IS NULL"))
            # Make NOT NULL
            op.alter_column('invoice_lines', 'station_id', nullable=False)
    
    # Clock templates
    if 'clock_templates' in existing_tables:
        existing_columns = [col['name'] for col in inspector.get_columns('clock_templates')]
        if 'station_id' not in existing_columns:
            op.add_column('clock_templates', sa.Column('station_id', sa.Integer(), nullable=True))
            op.create_foreign_key('fk_clock_templates_station_id', 'clock_templates', 'stations', ['station_id'], ['id'])
            op.create_index('ix_clock_templates_station_id', 'clock_templates', ['station_id'])
            conn.execute(text(f"UPDATE clock_templates SET station_id = {default_station_id} WHERE station_id IS NULL"))
            op.alter_column('clock_templates', 'station_id', nullable=False)
    
    # Dayparts
    if 'dayparts' in existing_tables:
        existing_columns = [col['name'] for col in inspector.get_columns('dayparts')]
        if 'station_id' not in existing_columns:
            op.add_column('dayparts', sa.Column('station_id', sa.Integer(), nullable=True))
            op.create_foreign_key('fk_dayparts_station_id', 'dayparts', 'stations', ['station_id'], ['id'])
            op.create_index('ix_dayparts_station_id', 'dayparts', ['station_id'])
            conn.execute(text(f"UPDATE dayparts SET station_id = {default_station_id} WHERE station_id IS NULL"))
            op.alter_column('dayparts', 'station_id', nullable=False)
    
    # Break structures
    if 'break_structures' in existing_tables:
        existing_columns = [col['name'] for col in inspector.get_columns('break_structures')]
        if 'station_id' not in existing_columns:
            op.add_column('break_structures', sa.Column('station_id', sa.Integer(), nullable=True))
            op.create_foreign_key('fk_break_structures_station_id', 'break_structures', 'stations', ['station_id'], ['id'])
            op.create_index('ix_break_structures_station_id', 'break_structures', ['station_id'])
            conn.execute(text(f"UPDATE break_structures SET station_id = {default_station_id} WHERE station_id IS NULL"))
            op.alter_column('break_structures', 'station_id', nullable=False)
    
    # Tracks (nullable - shared tracks have NULL)
    if 'tracks' in existing_tables:
        existing_columns = [col['name'] for col in inspector.get_columns('tracks')]
        if 'station_id' not in existing_columns:
            op.add_column('tracks', sa.Column('station_id', sa.Integer(), nullable=True))
            op.create_foreign_key('fk_tracks_station_id', 'tracks', 'stations', ['station_id'], ['id'])
            op.create_index('ix_tracks_station_id', 'tracks', ['station_id'])
    
    # Voice tracks
    if 'voice_tracks' in existing_tables:
        existing_columns = [col['name'] for col in inspector.get_columns('voice_tracks')]
        if 'station_id' not in existing_columns:
            op.add_column('voice_tracks', sa.Column('station_id', sa.Integer(), nullable=True))
            op.create_foreign_key('fk_voice_tracks_station_id', 'voice_tracks', 'stations', ['station_id'], ['id'])
            op.create_index('ix_voice_tracks_station_id', 'voice_tracks', ['station_id'])
            conn.execute(text(f"UPDATE voice_tracks SET station_id = {default_station_id} WHERE station_id IS NULL"))
            op.alter_column('voice_tracks', 'station_id', nullable=False)
    
    # Inventory slots
    if 'inventory_slots' in existing_tables:
        existing_columns = [col['name'] for col in inspector.get_columns('inventory_slots')]
        if 'station_id' not in existing_columns:
            op.add_column('inventory_slots', sa.Column('station_id', sa.Integer(), nullable=True))
            op.create_foreign_key('fk_inventory_slots_station_id', 'inventory_slots', 'stations', ['station_id'], ['id'])
            op.create_index('ix_inventory_slots_station_id', 'inventory_slots', ['station_id'])
            conn.execute(text(f"UPDATE inventory_slots SET station_id = {default_station_id} WHERE station_id IS NULL"))
            op.alter_column('inventory_slots', 'station_id', nullable=False)
            # Add unique constraint (date, hour, station_id)
            try:
                op.create_unique_constraint('uq_inventory_slots_date_hour_station', 'inventory_slots', ['date', 'hour', 'station_id'])
            except Exception:
                pass
    
    # Rotation rules
    if 'rotation_rules' in existing_tables:
        existing_columns = [col['name'] for col in inspector.get_columns('rotation_rules')]
        if 'station_id' not in existing_columns:
            op.add_column('rotation_rules', sa.Column('station_id', sa.Integer(), nullable=True))
            op.create_foreign_key('fk_rotation_rules_station_id', 'rotation_rules', 'stations', ['station_id'], ['id'])
            op.create_index('ix_rotation_rules_station_id', 'rotation_rules', ['station_id'])
            conn.execute(text(f"UPDATE rotation_rules SET station_id = {default_station_id} WHERE station_id IS NULL"))
            op.alter_column('rotation_rules', 'station_id', nullable=False)
    
    # Live reads
    if 'live_reads' in existing_tables:
        existing_columns = [col['name'] for col in inspector.get_columns('live_reads')]
        if 'station_id' not in existing_columns:
            op.add_column('live_reads', sa.Column('station_id', sa.Integer(), nullable=True))
            op.create_foreign_key('fk_live_reads_station_id', 'live_reads', 'stations', ['station_id'], ['id'])
            op.create_index('ix_live_reads_station_id', 'live_reads', ['station_id'])
            conn.execute(text(f"UPDATE live_reads SET station_id = {default_station_id} WHERE station_id IS NULL"))
            op.alter_column('live_reads', 'station_id', nullable=False)
    
    # Traffic logs (nullable for backward compatibility)
    if 'traffic_logs' in existing_tables:
        existing_columns = [col['name'] for col in inspector.get_columns('traffic_logs')]
        if 'station_id' not in existing_columns:
            op.add_column('traffic_logs', sa.Column('station_id', sa.Integer(), nullable=True))
            op.create_foreign_key('fk_traffic_logs_station_id', 'traffic_logs', 'stations', ['station_id'], ['id'])
            op.create_index('ix_traffic_logs_station_id', 'traffic_logs', ['station_id'])
    
    # Copy (nullable - shared or station-specific)
    if 'copy' in existing_tables:
        existing_columns = [col['name'] for col in inspector.get_columns('copy')]
        if 'station_id' not in existing_columns:
            op.add_column('copy', sa.Column('station_id', sa.Integer(), nullable=True))
            op.create_foreign_key('fk_copy_station_id', 'copy', 'stations', ['station_id'], ['id'])
            op.create_index('ix_copy_station_id', 'copy', ['station_id'])
    
    # Settings (nullable, update unique constraint)
    if 'settings' in existing_tables:
        existing_columns = [col['name'] for col in inspector.get_columns('settings')]
        if 'station_id' not in existing_columns:
            op.add_column('settings', sa.Column('station_id', sa.Integer(), nullable=True))
            op.create_foreign_key('fk_settings_station_id', 'settings', 'stations', ['station_id'], ['id'])
            op.create_index('ix_settings_station_id', 'settings', ['station_id'])
            # Drop old unique constraint if it exists
            try:
                op.drop_constraint('uq_settings_category_key', 'settings', type_='unique')
            except Exception:
                pass
            # Add new unique constraint (category, key, station_id)
            try:
                op.create_unique_constraint('uq_settings_category_key_station', 'settings', ['category', 'key', 'station_id'])
            except Exception:
                pass
    
    # Add LibreTime configuration fields to stations table
    if 'stations' in existing_tables:
        existing_columns = [col['name'] for col in inspector.get_columns('stations')]
        if 'libretime_config' not in existing_columns:
            op.add_column('stations', sa.Column('libretime_config', postgresql.JSONB(), nullable=True))
            op.create_index('ix_stations_libretime_config', 'stations', ['libretime_config'], postgresql_using='gin')


def downgrade():
    # Remove station_id columns and constraints
    from sqlalchemy import inspect
    conn = op.get_bind()
    inspector = inspect(conn)
    existing_tables = inspector.get_table_names()
    
    # Drop unique constraints first
    if 'settings' in existing_tables:
        try:
            op.drop_constraint('uq_settings_category_key_station', 'settings', type_='unique')
            op.create_unique_constraint('uq_settings_category_key', 'settings', ['category', 'key'])
        except Exception:
            pass
    
    if 'inventory_slots' in existing_tables:
        try:
            op.drop_constraint('uq_inventory_slots_date_hour_station', 'inventory_slots', type_='unique')
        except Exception:
            pass
    
    if 'daily_logs' in existing_tables:
        try:
            op.drop_constraint('uq_daily_logs_date_station', 'daily_logs', type_='unique')
        except Exception:
            pass
    
    # Drop LibreTime config from stations
    if 'stations' in existing_tables:
        existing_columns = [col['name'] for col in inspector.get_columns('stations')]
        if 'libretime_config' in existing_columns:
            try:
                op.drop_index('ix_stations_libretime_config', 'stations')
            except Exception:
                pass
            op.drop_column('stations', 'libretime_config')
    
    # Drop foreign keys and columns
    tables_to_downgrade = [
        'copy', 'traffic_logs', 'live_reads', 'rotation_rules', 'inventory_slots',
        'voice_tracks', 'tracks', 'break_structures', 'dayparts', 'clock_templates',
        'invoice_lines', 'order_lines', 'daily_logs', 'spots'
    ]
    
    for table_name in tables_to_downgrade:
        if table_name in existing_tables:
            existing_columns = [col['name'] for col in inspector.get_columns(table_name)]
            if 'station_id' in existing_columns:
                try:
                    op.drop_constraint(f'fk_{table_name}_station_id', table_name, type_='foreignkey')
                except Exception:
                    pass
                try:
                    op.drop_index(f'ix_{table_name}_station_id', table_name)
                except Exception:
                    pass
                op.drop_column(table_name, 'station_id')
    
    # Drop user_stations table
    if 'user_stations' in existing_tables:
        op.drop_table('user_stations')

