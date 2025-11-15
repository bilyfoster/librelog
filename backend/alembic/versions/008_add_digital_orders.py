"""Add digital orders table

Revision ID: 008
Revises: 007
Create Date: 2024-01-15 00:00:00.000000

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = '008'
down_revision = '007'
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Create enum types
    op.execute("CREATE TYPE platform AS ENUM ('WEBSITE', 'PODCAST', 'STREAMING', 'SOCIAL_MEDIA')")
    op.execute("CREATE TYPE digitalorderstatus AS ENUM ('DRAFT', 'ACTIVE', 'COMPLETED', 'CANCELLED')")
    
    # Create digital_orders table
    op.create_table('digital_orders',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('order_id', sa.Integer(), nullable=False),
        sa.Column('platform', postgresql.ENUM('WEBSITE', 'PODCAST', 'STREAMING', 'SOCIAL_MEDIA', name='platform'), nullable=False),
        sa.Column('impression_target', sa.Integer(), nullable=True),
        sa.Column('cpm', sa.Numeric(10, 2), nullable=True),
        sa.Column('cpc', sa.Numeric(10, 2), nullable=True),
        sa.Column('flat_rate', sa.Numeric(10, 2), nullable=True),
        sa.Column('creative_assets', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('delivered_impressions', sa.Integer(), nullable=True, server_default='0'),
        sa.Column('status', postgresql.ENUM('DRAFT', 'ACTIVE', 'COMPLETED', 'CANCELLED', name='digitalorderstatus'), nullable=False, server_default='DRAFT'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['order_id'], ['orders.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_digital_orders_id'), 'digital_orders', ['id'], unique=False)
    op.create_index(op.f('ix_digital_orders_order_id'), 'digital_orders', ['order_id'], unique=False)
    op.create_index(op.f('ix_digital_orders_status'), 'digital_orders', ['status'], unique=False)


def downgrade() -> None:
    # Drop digital_orders table
    op.drop_index(op.f('ix_digital_orders_status'), table_name='digital_orders')
    op.drop_index(op.f('ix_digital_orders_order_id'), table_name='digital_orders')
    op.drop_index(op.f('ix_digital_orders_id'), table_name='digital_orders')
    op.drop_table('digital_orders')
    
    # Drop enum types
    op.execute("DROP TYPE IF EXISTS digitalorderstatus")
    op.execute("DROP TYPE IF EXISTS platform")

