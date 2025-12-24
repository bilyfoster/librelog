"""Sales admin models - teams, offices, regions, stations, clusters

Revision ID: 022
Revises: 021
Create Date: 2025-11-20

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = '022'
down_revision = '021'
branch_labels = None
depends_on = None


def upgrade():
    # Check if tables already exist (they might have been created manually)
    from sqlalchemy import inspect
    conn = op.get_bind()
    inspector = inspect(conn)
    existing_tables = inspector.get_table_names()
    
    # Create sales_teams table
    if 'sales_teams' not in existing_tables:
        op.create_table(
        'sales_teams',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('name', sa.String(length=255), nullable=False),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=True, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_sales_teams_id'), 'sales_teams', ['id'], unique=False)
    op.create_index('ix_sales_teams_name', 'sales_teams', ['name'], unique=True)

    # Create sales_regions table
    op.create_table(
        'sales_regions',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('name', sa.String(length=255), nullable=False),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=True, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_sales_regions_id'), 'sales_regions', ['id'], unique=False)
    op.create_index('ix_sales_regions_name', 'sales_regions', ['name'], unique=True)

    # Create sales_offices table
    op.create_table(
        'sales_offices',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('name', sa.String(length=255), nullable=False),
        sa.Column('address', sa.Text(), nullable=True),
        sa.Column('region_id', sa.Integer(), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=True, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['region_id'], ['sales_regions.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_sales_offices_id'), 'sales_offices', ['id'], unique=False)
    op.create_index(op.f('ix_sales_offices_region_id'), 'sales_offices', ['region_id'], unique=False)

    # Create stations table
    op.create_table(
        'stations',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('call_letters', sa.String(length=10), nullable=False),
        sa.Column('frequency', sa.String(length=20), nullable=True),
        sa.Column('market', sa.String(length=255), nullable=True),
        sa.Column('format', sa.String(length=100), nullable=True),
        sa.Column('ownership', sa.String(length=255), nullable=True),
        sa.Column('contacts', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('rates', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('inventory_class', sa.String(length=50), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=True, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_stations_id'), 'stations', ['id'], unique=False)
    op.create_index('ix_stations_call_letters', 'stations', ['call_letters'], unique=True)
    op.create_index(op.f('ix_stations_frequency'), 'stations', ['frequency'], unique=False)
    op.create_index(op.f('ix_stations_market'), 'stations', ['market'], unique=False)
    op.create_index(op.f('ix_stations_inventory_class'), 'stations', ['inventory_class'], unique=False)

    # Create clusters table
    op.create_table(
        'clusters',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('name', sa.String(length=255), nullable=False),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=True, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_clusters_id'), 'clusters', ['id'], unique=False)
    op.create_index('ix_clusters_name', 'clusters', ['name'], unique=True)

    # Create association tables
    op.create_table(
        'sales_rep_teams',
        sa.Column('sales_rep_id', sa.Integer(), nullable=False),
        sa.Column('sales_team_id', sa.Integer(), nullable=False),
        sa.ForeignKeyConstraint(['sales_rep_id'], ['sales_reps.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['sales_team_id'], ['sales_teams.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('sales_rep_id', 'sales_team_id')
    )

    op.create_table(
        'sales_rep_offices',
        sa.Column('sales_rep_id', sa.Integer(), nullable=False),
        sa.Column('sales_office_id', sa.Integer(), nullable=False),
        sa.ForeignKeyConstraint(['sales_rep_id'], ['sales_reps.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['sales_office_id'], ['sales_offices.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('sales_rep_id', 'sales_office_id')
    )

    op.create_table(
        'sales_rep_regions',
        sa.Column('sales_rep_id', sa.Integer(), nullable=False),
        sa.Column('sales_region_id', sa.Integer(), nullable=False),
        sa.ForeignKeyConstraint(['sales_rep_id'], ['sales_reps.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['sales_region_id'], ['sales_regions.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('sales_rep_id', 'sales_region_id')
    )

    op.create_table(
        'station_clusters',
        sa.Column('station_id', sa.Integer(), nullable=False),
        sa.Column('cluster_id', sa.Integer(), nullable=False),
        sa.ForeignKeyConstraint(['station_id'], ['stations.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['cluster_id'], ['clusters.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('station_id', 'cluster_id')
    )

    # Add optional foreign keys to orders table
    op.add_column('orders', sa.Column('sales_team_id', sa.Integer(), nullable=True))
    op.add_column('orders', sa.Column('sales_office_id', sa.Integer(), nullable=True))
    op.add_column('orders', sa.Column('sales_region_id', sa.Integer(), nullable=True))
    op.create_foreign_key('fk_orders_sales_team_id', 'orders', 'sales_teams', ['sales_team_id'], ['id'])
    op.create_foreign_key('fk_orders_sales_office_id', 'orders', 'sales_offices', ['sales_office_id'], ['id'])
    op.create_foreign_key('fk_orders_sales_region_id', 'orders', 'sales_regions', ['sales_region_id'], ['id'])
    op.create_index(op.f('ix_orders_sales_team_id'), 'orders', ['sales_team_id'], unique=False)
    op.create_index(op.f('ix_orders_sales_office_id'), 'orders', ['sales_office_id'], unique=False)
    op.create_index(op.f('ix_orders_sales_region_id'), 'orders', ['sales_region_id'], unique=False)

    # Enhance agencies table
    op.add_column('agencies', sa.Column('website', sa.String(length=255), nullable=True))
    op.add_column('agencies', sa.Column('tax_id', sa.String(length=50), nullable=True))
    op.add_column('agencies', sa.Column('notes', sa.Text(), nullable=True))
    op.add_column('agencies', sa.Column('account_manager_id', sa.Integer(), nullable=True))
    op.create_foreign_key('fk_agencies_account_manager_id', 'agencies', 'users', ['account_manager_id'], ['id'])
    op.create_index(op.f('ix_agencies_tax_id'), 'agencies', ['tax_id'], unique=False)
    op.create_index(op.f('ix_agencies_account_manager_id'), 'agencies', ['account_manager_id'], unique=False)


def downgrade():
    # Remove agency enhancements
    op.drop_index(op.f('ix_agencies_account_manager_id'), table_name='agencies')
    op.drop_index(op.f('ix_agencies_tax_id'), table_name='agencies')
    op.drop_constraint('fk_agencies_account_manager_id', 'agencies', type_='foreignkey')
    op.drop_column('agencies', 'account_manager_id')
    op.drop_column('agencies', 'notes')
    op.drop_column('agencies', 'tax_id')
    op.drop_column('agencies', 'website')

    # Remove order foreign keys
    op.drop_index(op.f('ix_orders_sales_region_id'), table_name='orders')
    op.drop_index(op.f('ix_orders_sales_office_id'), table_name='orders')
    op.drop_index(op.f('ix_orders_sales_team_id'), table_name='orders')
    op.drop_constraint('fk_orders_sales_region_id', 'orders', type_='foreignkey')
    op.drop_constraint('fk_orders_sales_office_id', 'orders', type_='foreignkey')
    op.drop_constraint('fk_orders_sales_team_id', 'orders', type_='foreignkey')
    op.drop_column('orders', 'sales_region_id')
    op.drop_column('orders', 'sales_office_id')
    op.drop_column('orders', 'sales_team_id')

    # Drop association tables
    op.drop_table('station_clusters')
    op.drop_table('sales_rep_regions')
    op.drop_table('sales_rep_offices')
    op.drop_table('sales_rep_teams')

    # Drop main tables
    op.drop_index('ix_clusters_name', table_name='clusters')
    op.drop_index(op.f('ix_clusters_id'), table_name='clusters')
    op.drop_table('clusters')

    op.drop_index(op.f('ix_stations_inventory_class'), table_name='stations')
    op.drop_index(op.f('ix_stations_market'), table_name='stations')
    op.drop_index(op.f('ix_stations_frequency'), table_name='stations')
    op.drop_index('ix_stations_call_letters', table_name='stations')
    op.drop_index(op.f('ix_stations_id'), table_name='stations')
    op.drop_table('stations')

    op.drop_index(op.f('ix_sales_offices_region_id'), table_name='sales_offices')
    op.drop_index(op.f('ix_sales_offices_id'), table_name='sales_offices')
    op.drop_table('sales_offices')

    op.drop_index('ix_sales_regions_name', table_name='sales_regions')
    op.drop_index(op.f('ix_sales_regions_id'), table_name='sales_regions')
    op.drop_table('sales_regions')

    op.drop_index('ix_sales_teams_name', table_name='sales_teams')
    op.drop_index(op.f('ix_sales_teams_id'), table_name='sales_teams')
    op.drop_table('sales_teams')

