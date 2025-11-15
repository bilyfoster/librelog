"""Add traffic management tables

Revision ID: 002
Revises: 0001
Create Date: 2024-01-15 00:00:00.000000

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = '002'
down_revision = '0001'
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Create enum types
    op.execute("CREATE TYPE ratetype AS ENUM ('ROS', 'DAYPART', 'PROGRAM', 'FIXED_TIME')")
    op.execute("CREATE TYPE orderstatus AS ENUM ('DRAFT', 'PENDING', 'APPROVED', 'ACTIVE', 'COMPLETED', 'CANCELLED')")
    op.execute("CREATE TYPE approvalstatus AS ENUM ('PENDING', 'APPROVED', 'REJECTED', 'NOT_REQUIRED')")
    
    # Create advertisers table
    op.create_table('advertisers',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('name', sa.String(length=255), nullable=False),
        sa.Column('contact_name', sa.String(length=255), nullable=True),
        sa.Column('email', sa.String(length=255), nullable=True),
        sa.Column('phone', sa.String(length=50), nullable=True),
        sa.Column('address', sa.Text(), nullable=True),
        sa.Column('tax_id', sa.String(length=50), nullable=True),
        sa.Column('payment_terms', sa.String(length=100), nullable=True),
        sa.Column('credit_limit', sa.Numeric(10, 2), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=True, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_advertisers_id'), 'advertisers', ['id'], unique=False)
    op.create_index(op.f('ix_advertisers_name'), 'advertisers', ['name'], unique=False)
    op.create_index(op.f('ix_advertisers_email'), 'advertisers', ['email'], unique=False)
    op.create_index(op.f('ix_advertisers_active'), 'advertisers', ['active'], unique=False)
    
    # Create agencies table
    op.create_table('agencies',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('name', sa.String(length=255), nullable=False),
        sa.Column('contact_name', sa.String(length=255), nullable=True),
        sa.Column('email', sa.String(length=255), nullable=True),
        sa.Column('phone', sa.String(length=50), nullable=True),
        sa.Column('address', sa.Text(), nullable=True),
        sa.Column('commission_rate', sa.Numeric(5, 2), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=True, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_agencies_id'), 'agencies', ['id'], unique=False)
    op.create_index(op.f('ix_agencies_name'), 'agencies', ['name'], unique=False)
    op.create_index(op.f('ix_agencies_email'), 'agencies', ['email'], unique=False)
    op.create_index(op.f('ix_agencies_active'), 'agencies', ['active'], unique=False)
    
    # Create sales_reps table
    op.create_table('sales_reps',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('user_id', sa.Integer(), nullable=False),
        sa.Column('employee_id', sa.String(length=50), nullable=True),
        sa.Column('commission_rate', sa.Numeric(5, 2), nullable=True),
        sa.Column('sales_goal', sa.Numeric(10, 2), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=True, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('user_id'),
        sa.UniqueConstraint('employee_id')
    )
    op.create_index(op.f('ix_sales_reps_id'), 'sales_reps', ['id'], unique=False)
    op.create_index(op.f('ix_sales_reps_user_id'), 'sales_reps', ['user_id'], unique=True)
    op.create_index(op.f('ix_sales_reps_employee_id'), 'sales_reps', ['employee_id'], unique=True)
    op.create_index(op.f('ix_sales_reps_active'), 'sales_reps', ['active'], unique=False)
    
    # Create order_templates table
    op.create_table('order_templates',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('name', sa.String(length=255), nullable=False),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('default_spot_lengths', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('default_rate_type', sa.String(length=20), nullable=True),
        sa.Column('default_rates', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('created_by', sa.Integer(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['created_by'], ['users.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_order_templates_id'), 'order_templates', ['id'], unique=False)
    op.create_index(op.f('ix_order_templates_name'), 'order_templates', ['name'], unique=False)
    
    # Create orders table
    op.create_table('orders',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('order_number', sa.String(length=50), nullable=False),
        sa.Column('campaign_id', sa.Integer(), nullable=True),
        sa.Column('advertiser_id', sa.Integer(), nullable=False),
        sa.Column('agency_id', sa.Integer(), nullable=True),
        sa.Column('sales_rep_id', sa.Integer(), nullable=True),
        sa.Column('start_date', sa.Date(), nullable=False),
        sa.Column('end_date', sa.Date(), nullable=False),
        sa.Column('spot_lengths', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('total_spots', sa.Integer(), nullable=True, server_default='0'),
        sa.Column('rate_type', postgresql.ENUM('ROS', 'DAYPART', 'PROGRAM', 'FIXED_TIME', name='ratetype'), nullable=False, server_default='ROS'),
        sa.Column('rates', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('total_value', sa.Numeric(10, 2), nullable=True, server_default='0'),
        sa.Column('status', postgresql.ENUM('DRAFT', 'PENDING', 'APPROVED', 'ACTIVE', 'COMPLETED', 'CANCELLED', name='orderstatus'), nullable=False, server_default='DRAFT'),
        sa.Column('approval_status', postgresql.ENUM('PENDING', 'APPROVED', 'REJECTED', 'NOT_REQUIRED', name='approvalstatus'), nullable=False, server_default='NOT_REQUIRED'),
        sa.Column('approved_by', sa.Integer(), nullable=True),
        sa.Column('approved_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['advertiser_id'], ['advertisers.id'], ),
        sa.ForeignKeyConstraint(['agency_id'], ['agencies.id'], ),
        sa.ForeignKeyConstraint(['campaign_id'], ['campaigns.id'], ),
        sa.ForeignKeyConstraint(['sales_rep_id'], ['sales_reps.id'], ),
        sa.ForeignKeyConstraint(['approved_by'], ['users.id'], ),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('order_number')
    )
    op.create_index(op.f('ix_orders_id'), 'orders', ['id'], unique=False)
    op.create_index(op.f('ix_orders_order_number'), 'orders', ['order_number'], unique=True)
    op.create_index(op.f('ix_orders_campaign_id'), 'orders', ['campaign_id'], unique=False)
    op.create_index(op.f('ix_orders_advertiser_id'), 'orders', ['advertiser_id'], unique=False)
    op.create_index(op.f('ix_orders_agency_id'), 'orders', ['agency_id'], unique=False)
    op.create_index(op.f('ix_orders_sales_rep_id'), 'orders', ['sales_rep_id'], unique=False)
    op.create_index(op.f('ix_orders_start_date'), 'orders', ['start_date'], unique=False)
    op.create_index(op.f('ix_orders_end_date'), 'orders', ['end_date'], unique=False)
    op.create_index(op.f('ix_orders_status'), 'orders', ['status'], unique=False)
    
    # Extend campaigns table with new fields
    op.add_column('campaigns', sa.Column('order_number', sa.String(length=50), nullable=True))
    op.add_column('campaigns', sa.Column('contract_number', sa.String(length=50), nullable=True))
    op.add_column('campaigns', sa.Column('insertion_order_url', sa.Text(), nullable=True))
    op.add_column('campaigns', sa.Column('spot_lengths', postgresql.JSONB(astext_type=sa.Text()), nullable=True))
    op.add_column('campaigns', sa.Column('rate_type', postgresql.ENUM('ROS', 'DAYPART', 'PROGRAM', 'FIXED_TIME', name='ratetype'), nullable=True))
    op.add_column('campaigns', sa.Column('rates', postgresql.JSONB(astext_type=sa.Text()), nullable=True))
    op.add_column('campaigns', sa.Column('scripts', postgresql.JSONB(astext_type=sa.Text()), nullable=True))
    op.add_column('campaigns', sa.Column('copy_instructions', sa.Text(), nullable=True))
    op.add_column('campaigns', sa.Column('traffic_restrictions', postgresql.JSONB(astext_type=sa.Text()), nullable=True))
    op.add_column('campaigns', sa.Column('approval_status', postgresql.ENUM('PENDING', 'APPROVED', 'REJECTED', 'NOT_REQUIRED', name='approvalstatus'), nullable=True, server_default='NOT_REQUIRED'))
    op.add_column('campaigns', sa.Column('approved_by', sa.Integer(), nullable=True))
    op.add_column('campaigns', sa.Column('approved_at', sa.DateTime(timezone=True), nullable=True))
    op.create_index(op.f('ix_campaigns_order_number'), 'campaigns', ['order_number'], unique=True)
    op.create_index(op.f('ix_campaigns_contract_number'), 'campaigns', ['contract_number'], unique=False)
    op.create_foreign_key('campaigns_approved_by_fkey', 'campaigns', 'users', ['approved_by'], ['id'])


def downgrade() -> None:
    # Remove extended fields from campaigns
    op.drop_constraint('campaigns_approved_by_fkey', 'campaigns', type_='foreignkey')
    op.drop_index(op.f('ix_campaigns_contract_number'), table_name='campaigns')
    op.drop_index(op.f('ix_campaigns_order_number'), table_name='campaigns')
    op.drop_column('campaigns', 'approved_at')
    op.drop_column('campaigns', 'approved_by')
    op.drop_column('campaigns', 'approval_status')
    op.drop_column('campaigns', 'traffic_restrictions')
    op.drop_column('campaigns', 'copy_instructions')
    op.drop_column('campaigns', 'scripts')
    op.drop_column('campaigns', 'rates')
    op.drop_column('campaigns', 'rate_type')
    op.drop_column('campaigns', 'spot_lengths')
    op.drop_column('campaigns', 'insertion_order_url')
    op.drop_column('campaigns', 'contract_number')
    op.drop_column('campaigns', 'order_number')
    
    # Drop orders table
    op.drop_index(op.f('ix_orders_status'), table_name='orders')
    op.drop_index(op.f('ix_orders_end_date'), table_name='orders')
    op.drop_index(op.f('ix_orders_start_date'), table_name='orders')
    op.drop_index(op.f('ix_orders_sales_rep_id'), table_name='orders')
    op.drop_index(op.f('ix_orders_agency_id'), table_name='orders')
    op.drop_index(op.f('ix_orders_advertiser_id'), table_name='orders')
    op.drop_index(op.f('ix_orders_campaign_id'), table_name='orders')
    op.drop_index(op.f('ix_orders_order_number'), table_name='orders')
    op.drop_index(op.f('ix_orders_id'), table_name='orders')
    op.drop_table('orders')
    
    # Drop order_templates table
    op.drop_index(op.f('ix_order_templates_name'), table_name='order_templates')
    op.drop_index(op.f('ix_order_templates_id'), table_name='order_templates')
    op.drop_table('order_templates')
    
    # Drop sales_reps table
    op.drop_index(op.f('ix_sales_reps_active'), table_name='sales_reps')
    op.drop_index(op.f('ix_sales_reps_employee_id'), table_name='sales_reps')
    op.drop_index(op.f('ix_sales_reps_user_id'), table_name='sales_reps')
    op.drop_index(op.f('ix_sales_reps_id'), table_name='sales_reps')
    op.drop_table('sales_reps')
    
    # Drop agencies table
    op.drop_index(op.f('ix_agencies_active'), table_name='agencies')
    op.drop_index(op.f('ix_agencies_email'), table_name='agencies')
    op.drop_index(op.f('ix_agencies_name'), table_name='agencies')
    op.drop_index(op.f('ix_agencies_id'), table_name='agencies')
    op.drop_table('agencies')
    
    # Drop advertisers table
    op.drop_index(op.f('ix_advertisers_active'), table_name='advertisers')
    op.drop_index(op.f('ix_advertisers_email'), table_name='advertisers')
    op.drop_index(op.f('ix_advertisers_name'), table_name='advertisers')
    op.drop_index(op.f('ix_advertisers_id'), table_name='advertisers')
    op.drop_table('advertisers')
    
    # Drop enum types
    op.execute("DROP TYPE IF EXISTS approvalstatus")
    op.execute("DROP TYPE IF EXISTS orderstatus")
    op.execute("DROP TYPE IF EXISTS ratetype")

