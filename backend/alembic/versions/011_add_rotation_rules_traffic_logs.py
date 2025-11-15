"""Add rotation rules, traffic logs, and daypart categories tables

Revision ID: 011
Revises: 010
Create Date: 2024-01-22 00:00:00.000000

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = '011'
down_revision = '010'
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Create daypart_categories table
    op.create_table('daypart_categories',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('name', sa.String(length=100), nullable=False),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('color', sa.String(length=7), nullable=True),
        sa.Column('icon', sa.String(length=50), nullable=True),
        sa.Column('sort_order', sa.Integer(), nullable=True, server_default='0'),
        sa.Column('active', sa.Boolean(), nullable=True, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_daypart_categories_id'), 'daypart_categories', ['id'], unique=False)
    op.create_index(op.f('ix_daypart_categories_name'), 'daypart_categories', ['name'], unique=True)
    op.create_index(op.f('ix_daypart_categories_sort_order'), 'daypart_categories', ['sort_order'], unique=False)
    op.create_index(op.f('ix_daypart_categories_active'), 'daypart_categories', ['active'], unique=False)

    # Create rotation_rules table
    op.create_table('rotation_rules',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('name', sa.String(length=100), nullable=False),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('rotation_type', sa.String(length=20), nullable=False, server_default='SEQUENTIAL'),
        sa.Column('daypart_id', sa.Integer(), nullable=True),
        sa.Column('campaign_id', sa.Integer(), nullable=True),
        sa.Column('min_separation', sa.Integer(), nullable=True, server_default='0'),
        sa.Column('max_per_hour', sa.Integer(), nullable=True),
        sa.Column('max_per_day', sa.Integer(), nullable=True),
        sa.Column('weights', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('exclude_days', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('exclude_times', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('priority', sa.Integer(), nullable=True, server_default='0'),
        sa.Column('active', sa.Boolean(), nullable=True, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.ForeignKeyConstraint(['campaign_id'], ['campaigns.id'], ),
        sa.ForeignKeyConstraint(['daypart_id'], ['dayparts.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_rotation_rules_id'), 'rotation_rules', ['id'], unique=False)
    op.create_index(op.f('ix_rotation_rules_name'), 'rotation_rules', ['name'], unique=False)
    op.create_index(op.f('ix_rotation_rules_daypart_id'), 'rotation_rules', ['daypart_id'], unique=False)
    op.create_index(op.f('ix_rotation_rules_campaign_id'), 'rotation_rules', ['campaign_id'], unique=False)
    op.create_index(op.f('ix_rotation_rules_active'), 'rotation_rules', ['active'], unique=False)

    # Create traffic_logs table
    op.create_table('traffic_logs',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('log_type', sa.String(length=50), nullable=False),
        sa.Column('log_id', sa.Integer(), nullable=True),
        sa.Column('spot_id', sa.Integer(), nullable=True),
        sa.Column('order_id', sa.Integer(), nullable=True),
        sa.Column('campaign_id', sa.Integer(), nullable=True),
        sa.Column('copy_id', sa.Integer(), nullable=True),
        sa.Column('user_id', sa.Integer(), nullable=False),
        sa.Column('message', sa.Text(), nullable=False),
        sa.Column('metadata', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('ip_address', sa.String(length=45), nullable=True),
        sa.Column('user_agent', sa.Text(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.ForeignKeyConstraint(['campaign_id'], ['campaigns.id'], ),
        sa.ForeignKeyConstraint(['copy_id'], ['copy.id'], ),
        sa.ForeignKeyConstraint(['log_id'], ['daily_logs.id'], ),
        sa.ForeignKeyConstraint(['order_id'], ['orders.id'], ),
        sa.ForeignKeyConstraint(['spot_id'], ['spots.id'], ),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ),
        sa.PrimaryKeyConstraint('id')
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

    # Add category_id to dayparts table
    op.add_column('dayparts', sa.Column('category_id', sa.Integer(), nullable=True))
    op.add_column('dayparts', sa.Column('description', sa.String(length=255), nullable=True))
    op.create_foreign_key('fk_dayparts_category', 'dayparts', 'daypart_categories', ['category_id'], ['id'])
    op.create_index(op.f('ix_dayparts_category_id'), 'dayparts', ['category_id'], unique=False)


def downgrade() -> None:
    # Remove category_id from dayparts
    op.drop_index(op.f('ix_dayparts_category_id'), table_name='dayparts')
    op.drop_constraint('fk_dayparts_category', 'dayparts', type_='foreignkey')
    op.drop_column('dayparts', 'description')
    op.drop_column('dayparts', 'category_id')

    # Drop traffic_logs table
    op.drop_index(op.f('ix_traffic_logs_created_at'), table_name='traffic_logs')
    op.drop_index(op.f('ix_traffic_logs_user_id'), table_name='traffic_logs')
    op.drop_index(op.f('ix_traffic_logs_copy_id'), table_name='traffic_logs')
    op.drop_index(op.f('ix_traffic_logs_campaign_id'), table_name='traffic_logs')
    op.drop_index(op.f('ix_traffic_logs_order_id'), table_name='traffic_logs')
    op.drop_index(op.f('ix_traffic_logs_spot_id'), table_name='traffic_logs')
    op.drop_index(op.f('ix_traffic_logs_log_id'), table_name='traffic_logs')
    op.drop_index(op.f('ix_traffic_logs_log_type'), table_name='traffic_logs')
    op.drop_index(op.f('ix_traffic_logs_id'), table_name='traffic_logs')
    op.drop_table('traffic_logs')

    # Drop rotation_rules table
    op.drop_index(op.f('ix_rotation_rules_active'), table_name='rotation_rules')
    op.drop_index(op.f('ix_rotation_rules_campaign_id'), table_name='rotation_rules')
    op.drop_index(op.f('ix_rotation_rules_daypart_id'), table_name='rotation_rules')
    op.drop_index(op.f('ix_rotation_rules_name'), table_name='rotation_rules')
    op.drop_index(op.f('ix_rotation_rules_id'), table_name='rotation_rules')
    op.drop_table('rotation_rules')

    # Drop daypart_categories table
    op.drop_index(op.f('ix_daypart_categories_active'), table_name='daypart_categories')
    op.drop_index(op.f('ix_daypart_categories_sort_order'), table_name='daypart_categories')
    op.drop_index(op.f('ix_daypart_categories_name'), table_name='daypart_categories')
    op.drop_index(op.f('ix_daypart_categories_id'), table_name='daypart_categories')
    op.drop_table('daypart_categories')

