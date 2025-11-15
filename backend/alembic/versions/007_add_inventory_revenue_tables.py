"""Add inventory and revenue tables

Revision ID: 007
Revises: 006
Create Date: 2024-01-15 00:00:00.000000

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = '007'
down_revision = '006'
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Create enum type
    op.execute("CREATE TYPE periodtype AS ENUM ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY')")
    
    # Create inventory_slots table
    op.create_table('inventory_slots',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('date', sa.Date(), nullable=False),
        sa.Column('hour', sa.Integer(), nullable=False),
        sa.Column('break_position', sa.String(length=10), nullable=True),
        sa.Column('daypart', sa.String(length=50), nullable=True),
        sa.Column('available', sa.Integer(), nullable=True, server_default='3600'),
        sa.Column('booked', sa.Integer(), nullable=True, server_default='0'),
        sa.Column('sold_out', sa.Boolean(), nullable=True, server_default='false'),
        sa.Column('revenue', sa.Numeric(10, 2), nullable=True, server_default='0'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.CheckConstraint('hour >= 0 AND hour <= 23', name='inventory_slots_hour_check'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_inventory_slots_id'), 'inventory_slots', ['id'], unique=False)
    op.create_index(op.f('ix_inventory_slots_date'), 'inventory_slots', ['date'], unique=False)
    op.create_index(op.f('ix_inventory_slots_hour'), 'inventory_slots', ['hour'], unique=False)
    op.create_index(op.f('ix_inventory_slots_sold_out'), 'inventory_slots', ['sold_out'], unique=False)
    
    # Create sales_goals table
    op.create_table('sales_goals',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('sales_rep_id', sa.Integer(), nullable=False),
        sa.Column('period', postgresql.ENUM('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY', name='periodtype'), nullable=False),
        sa.Column('target_date', sa.Date(), nullable=False),
        sa.Column('goal_amount', sa.Numeric(10, 2), nullable=False),
        sa.Column('actual_amount', sa.Numeric(10, 2), nullable=True, server_default='0'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['sales_rep_id'], ['sales_reps.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_sales_goals_id'), 'sales_goals', ['id'], unique=False)
    op.create_index(op.f('ix_sales_goals_sales_rep_id'), 'sales_goals', ['sales_rep_id'], unique=False)
    op.create_index(op.f('ix_sales_goals_period'), 'sales_goals', ['period'], unique=False)
    op.create_index(op.f('ix_sales_goals_target_date'), 'sales_goals', ['target_date'], unique=False)


def downgrade() -> None:
    # Drop sales_goals table
    op.drop_index(op.f('ix_sales_goals_target_date'), table_name='sales_goals')
    op.drop_index(op.f('ix_sales_goals_period'), table_name='sales_goals')
    op.drop_index(op.f('ix_sales_goals_sales_rep_id'), table_name='sales_goals')
    op.drop_index(op.f('ix_sales_goals_id'), table_name='sales_goals')
    op.drop_table('sales_goals')
    
    # Drop inventory_slots table
    op.drop_index(op.f('ix_inventory_slots_sold_out'), table_name='inventory_slots')
    op.drop_index(op.f('ix_inventory_slots_hour'), table_name='inventory_slots')
    op.drop_index(op.f('ix_inventory_slots_date'), table_name='inventory_slots')
    op.drop_index(op.f('ix_inventory_slots_id'), table_name='inventory_slots')
    op.drop_table('inventory_slots')
    
    # Drop enum type
    op.execute("DROP TYPE IF EXISTS periodtype")

