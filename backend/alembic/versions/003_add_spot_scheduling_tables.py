"""Add spot scheduling tables

Revision ID: 003
Revises: 002
Create Date: 2024-01-15 00:00:00.000000

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = '003'
down_revision = '002'
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Create enum types
    op.execute("CREATE TYPE breakposition AS ENUM ('A', 'B', 'C', 'D', 'E')")
    op.execute("CREATE TYPE spotdaypart AS ENUM ('MORNING_DRIVE', 'MIDDAY', 'AFTERNOON_DRIVE', 'EVENING', 'OVERNIGHT')")
    op.execute("CREATE TYPE spotstatus AS ENUM ('SCHEDULED', 'AIRED', 'MISSED', 'MAKEGOOD')")
    
    # Create dayparts table
    op.create_table('dayparts',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('name', sa.String(length=100), nullable=False),
        sa.Column('start_time', sa.Time(), nullable=False),
        sa.Column('end_time', sa.Time(), nullable=False),
        sa.Column('days_of_week', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=True, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_dayparts_id'), 'dayparts', ['id'], unique=False)
    op.create_index(op.f('ix_dayparts_name'), 'dayparts', ['name'], unique=False)
    op.create_index(op.f('ix_dayparts_active'), 'dayparts', ['active'], unique=False)
    
    # Create break_structures table
    op.create_table('break_structures',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('name', sa.String(length=100), nullable=False),
        sa.Column('hour', sa.Integer(), nullable=False),
        sa.Column('break_positions', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=True, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.CheckConstraint('hour >= 0 AND hour <= 23', name='break_structures_hour_check'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_break_structures_id'), 'break_structures', ['id'], unique=False)
    op.create_index(op.f('ix_break_structures_name'), 'break_structures', ['name'], unique=False)
    op.create_index(op.f('ix_break_structures_hour'), 'break_structures', ['hour'], unique=False)
    op.create_index(op.f('ix_break_structures_active'), 'break_structures', ['active'], unique=False)
    
    # Create spots table
    op.create_table('spots',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('order_id', sa.Integer(), nullable=False),
        sa.Column('campaign_id', sa.Integer(), nullable=True),
        sa.Column('scheduled_date', sa.Date(), nullable=False),
        sa.Column('scheduled_time', sa.String(length=8), nullable=False),
        sa.Column('spot_length', sa.Integer(), nullable=False),
        sa.Column('break_position', postgresql.ENUM('A', 'B', 'C', 'D', 'E', name='breakposition'), nullable=True),
        sa.Column('daypart', postgresql.ENUM('MORNING_DRIVE', 'MIDDAY', 'AFTERNOON_DRIVE', 'EVENING', 'OVERNIGHT', name='spotdaypart'), nullable=True),
        sa.Column('status', postgresql.ENUM('SCHEDULED', 'AIRED', 'MISSED', 'MAKEGOOD', name='spotstatus'), nullable=False, server_default='SCHEDULED'),
        sa.Column('actual_air_time', sa.DateTime(timezone=True), nullable=True),
        sa.Column('makegood_of_id', sa.Integer(), nullable=True),
        sa.Column('conflict_resolved', sa.Boolean(), nullable=True, server_default='false'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['order_id'], ['orders.id'], ),
        sa.ForeignKeyConstraint(['campaign_id'], ['campaigns.id'], ),
        sa.ForeignKeyConstraint(['makegood_of_id'], ['spots.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_spots_id'), 'spots', ['id'], unique=False)
    op.create_index(op.f('ix_spots_order_id'), 'spots', ['order_id'], unique=False)
    op.create_index(op.f('ix_spots_campaign_id'), 'spots', ['campaign_id'], unique=False)
    op.create_index(op.f('ix_spots_scheduled_date'), 'spots', ['scheduled_date'], unique=False)
    op.create_index(op.f('ix_spots_daypart'), 'spots', ['daypart'], unique=False)
    op.create_index(op.f('ix_spots_status'), 'spots', ['status'], unique=False)
    
    # Extend daily_logs table with new fields
    op.add_column('daily_logs', sa.Column('locked', sa.Boolean(), nullable=True, server_default='false'))
    op.add_column('daily_logs', sa.Column('locked_at', sa.DateTime(timezone=True), nullable=True))
    op.add_column('daily_logs', sa.Column('locked_by', sa.Integer(), nullable=True))
    op.add_column('daily_logs', sa.Column('conflicts', postgresql.JSONB(astext_type=sa.Text()), nullable=True))
    op.add_column('daily_logs', sa.Column('oversell_warnings', postgresql.JSONB(astext_type=sa.Text()), nullable=True))
    op.create_index(op.f('ix_daily_logs_locked'), 'daily_logs', ['locked'], unique=False)
    op.create_foreign_key('daily_logs_locked_by_fkey', 'daily_logs', 'users', ['locked_by'], ['id'])


def downgrade() -> None:
    # Remove extended fields from daily_logs
    op.drop_constraint('daily_logs_locked_by_fkey', 'daily_logs', type_='foreignkey')
    op.drop_index(op.f('ix_daily_logs_locked'), table_name='daily_logs')
    op.drop_column('daily_logs', 'oversell_warnings')
    op.drop_column('daily_logs', 'conflicts')
    op.drop_column('daily_logs', 'locked_by')
    op.drop_column('daily_logs', 'locked_at')
    op.drop_column('daily_logs', 'locked')
    
    # Drop spots table
    op.drop_index(op.f('ix_spots_status'), table_name='spots')
    op.drop_index(op.f('ix_spots_daypart'), table_name='spots')
    op.drop_index(op.f('ix_spots_scheduled_date'), table_name='spots')
    op.drop_index(op.f('ix_spots_campaign_id'), table_name='spots')
    op.drop_index(op.f('ix_spots_order_id'), table_name='spots')
    op.drop_index(op.f('ix_spots_id'), table_name='spots')
    op.drop_table('spots')
    
    # Drop break_structures table
    op.drop_index(op.f('ix_break_structures_active'), table_name='break_structures')
    op.drop_index(op.f('ix_break_structures_hour'), table_name='break_structures')
    op.drop_index(op.f('ix_break_structures_name'), table_name='break_structures')
    op.drop_index(op.f('ix_break_structures_id'), table_name='break_structures')
    op.drop_table('break_structures')
    
    # Drop dayparts table
    op.drop_index(op.f('ix_dayparts_active'), table_name='dayparts')
    op.drop_index(op.f('ix_dayparts_name'), table_name='dayparts')
    op.drop_index(op.f('ix_dayparts_id'), table_name='dayparts')
    op.drop_table('dayparts')
    
    # Drop enum types
    op.execute("DROP TYPE IF EXISTS spotstatus")
    op.execute("DROP TYPE IF EXISTS spotdaypart")
    op.execute("DROP TYPE IF EXISTS breakposition")

