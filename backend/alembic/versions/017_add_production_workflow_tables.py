"""Add production workflow tables

Revision ID: 017
Revises: 016
Create Date: 2025-01-20 10:00:00.000000

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = '017'
down_revision = '016'
branch_labels = None
depends_on = None


def upgrade():
    # Create production_orders table
    op.create_table(
        'production_orders',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('po_number', sa.String(length=50), nullable=False),
        sa.Column('copy_id', sa.Integer(), nullable=False),
        sa.Column('order_id', sa.Integer(), nullable=True),
        sa.Column('campaign_id', sa.Integer(), nullable=True),
        sa.Column('advertiser_id', sa.Integer(), nullable=False),
        sa.Column('client_name', sa.String(length=255), nullable=False),
        sa.Column('campaign_title', sa.String(length=255), nullable=True),
        sa.Column('start_date', sa.Date(), nullable=True),
        sa.Column('end_date', sa.Date(), nullable=True),
        sa.Column('budget', sa.Numeric(precision=10, scale=2), nullable=True),
        sa.Column('contract_number', sa.String(length=100), nullable=True),
        sa.Column('spot_lengths', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('deliverables', sa.Text(), nullable=True),
        sa.Column('copy_requirements', sa.Text(), nullable=True),
        sa.Column('talent_needs', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('audio_references', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('instructions', sa.Text(), nullable=True),
        sa.Column('deadline', sa.DateTime(timezone=True), nullable=True),
        sa.Column('stations', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('version_count', sa.Integer(), nullable=True, server_default='1'),
        sa.Column('order_type', sa.String(length=20), nullable=False, server_default='new_spot'),
        sa.Column('status', sa.String(length=20), nullable=False, server_default='PENDING'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('completed_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('delivered_at', sa.DateTime(timezone=True), nullable=True),
        sa.ForeignKeyConstraint(['copy_id'], ['copy.id'], ),
        sa.ForeignKeyConstraint(['order_id'], ['orders.id'], ),
        sa.ForeignKeyConstraint(['campaign_id'], ['campaigns.id'], ),
        sa.ForeignKeyConstraint(['advertiser_id'], ['advertisers.id'], ),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('po_number'),
        sa.UniqueConstraint('copy_id')
    )
    op.create_index(op.f('ix_production_orders_po_number'), 'production_orders', ['po_number'], unique=True)
    op.create_index(op.f('ix_production_orders_copy_id'), 'production_orders', ['copy_id'], unique=True)
    op.create_index(op.f('ix_production_orders_order_id'), 'production_orders', ['order_id'], unique=False)
    op.create_index(op.f('ix_production_orders_campaign_id'), 'production_orders', ['campaign_id'], unique=False)
    op.create_index(op.f('ix_production_orders_advertiser_id'), 'production_orders', ['advertiser_id'], unique=False)
    op.create_index(op.f('ix_production_orders_status'), 'production_orders', ['status'], unique=False)
    op.create_index(op.f('ix_production_orders_deadline'), 'production_orders', ['deadline'], unique=False)

    # Create production_assignments table
    op.create_table(
        'production_assignments',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('production_order_id', sa.Integer(), nullable=False),
        sa.Column('user_id', sa.Integer(), nullable=False),
        sa.Column('assignment_type', sa.String(length=20), nullable=False),
        sa.Column('status', sa.String(length=20), nullable=False, server_default='PENDING'),
        sa.Column('notes', sa.Text(), nullable=True),
        sa.Column('assigned_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('accepted_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('completed_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['production_order_id'], ['production_orders.id'], ),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_production_assignments_production_order_id'), 'production_assignments', ['production_order_id'], unique=False)
    op.create_index(op.f('ix_production_assignments_user_id'), 'production_assignments', ['user_id'], unique=False)
    op.create_index(op.f('ix_production_assignments_assignment_type'), 'production_assignments', ['assignment_type'], unique=False)
    op.create_index(op.f('ix_production_assignments_status'), 'production_assignments', ['status'], unique=False)

    # Create voice_talent_requests table
    op.create_table(
        'voice_talent_requests',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('production_order_id', sa.Integer(), nullable=False),
        sa.Column('talent_user_id', sa.Integer(), nullable=True),
        sa.Column('talent_type', sa.String(length=20), nullable=False),
        sa.Column('script', sa.Text(), nullable=False),
        sa.Column('pronunciation_guides', sa.Text(), nullable=True),
        sa.Column('talent_instructions', sa.Text(), nullable=True),
        sa.Column('status', sa.String(length=20), nullable=False, server_default='PENDING'),
        sa.Column('takes', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('assigned_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('deadline', sa.DateTime(timezone=True), nullable=True),
        sa.Column('completed_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['production_order_id'], ['production_orders.id'], ),
        sa.ForeignKeyConstraint(['talent_user_id'], ['users.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_voice_talent_requests_production_order_id'), 'voice_talent_requests', ['production_order_id'], unique=False)
    op.create_index(op.f('ix_voice_talent_requests_talent_user_id'), 'voice_talent_requests', ['talent_user_id'], unique=False)
    op.create_index(op.f('ix_voice_talent_requests_talent_type'), 'voice_talent_requests', ['talent_type'], unique=False)
    op.create_index(op.f('ix_voice_talent_requests_status'), 'voice_talent_requests', ['status'], unique=False)
    op.create_index(op.f('ix_voice_talent_requests_deadline'), 'voice_talent_requests', ['deadline'], unique=False)

    # Create production_revisions table
    op.create_table(
        'production_revisions',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('production_order_id', sa.Integer(), nullable=False),
        sa.Column('revision_number', sa.Integer(), nullable=False),
        sa.Column('requested_by', sa.Integer(), nullable=False),
        sa.Column('reason', sa.Text(), nullable=False),
        sa.Column('notes', sa.Text(), nullable=True),
        sa.Column('previous_revision_id', sa.Integer(), nullable=True),
        sa.Column('requested_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('completed_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['production_order_id'], ['production_orders.id'], ),
        sa.ForeignKeyConstraint(['requested_by'], ['users.id'], ),
        sa.ForeignKeyConstraint(['previous_revision_id'], ['production_revisions.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_production_revisions_production_order_id'), 'production_revisions', ['production_order_id'], unique=False)

    # Create production_comments table
    op.create_table(
        'production_comments',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('production_order_id', sa.Integer(), nullable=False),
        sa.Column('comment_text', sa.Text(), nullable=False),
        sa.Column('author_id', sa.Integer(), nullable=False),
        sa.Column('parent_comment_id', sa.Integer(), nullable=True),
        sa.Column('attachments', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('is_internal', sa.Boolean(), nullable=True, server_default='false'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['production_order_id'], ['production_orders.id'], ),
        sa.ForeignKeyConstraint(['author_id'], ['users.id'], ),
        sa.ForeignKeyConstraint(['parent_comment_id'], ['production_comments.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_production_comments_production_order_id'), 'production_comments', ['production_order_id'], unique=False)

    # Add production workflow fields to copy table
    op.add_column('copy', sa.Column('production_order_id', sa.Integer(), nullable=True))
    op.add_column('copy', sa.Column('copy_status', sa.String(length=20), nullable=False, server_default='draft'))
    op.add_column('copy', sa.Column('needs_production', sa.Boolean(), nullable=True, server_default='false'))
    op.add_column('copy', sa.Column('copy_approval_status', sa.String(length=20), nullable=False, server_default='pending'))
    op.add_column('copy', sa.Column('script_approved_at', sa.DateTime(timezone=True), nullable=True))
    op.add_column('copy', sa.Column('script_approved_by', sa.Integer(), nullable=True))
    
    op.create_foreign_key('fk_copy_production_order', 'copy', 'production_orders', ['production_order_id'], ['id'])
    op.create_foreign_key('fk_copy_script_approver', 'copy', 'users', ['script_approved_by'], ['id'])
    op.create_index(op.f('ix_copy_production_order_id'), 'copy', ['production_order_id'], unique=True)
    op.create_index(op.f('ix_copy_copy_status'), 'copy', ['copy_status'], unique=False)
    op.create_index(op.f('ix_copy_needs_production'), 'copy', ['needs_production'], unique=False)
    op.create_index(op.f('ix_copy_copy_approval_status'), 'copy', ['copy_approval_status'], unique=False)

    # Update users table constraint to allow new roles
    op.drop_constraint('users_role_check', 'users', type_='check')
    op.create_check_constraint(
        'users_role_check',
        'users',
        "role IN ('admin', 'producer', 'dj', 'sales', 'production_director', 'voice_talent')"
    )


def downgrade():
    # Remove production workflow fields from copy table
    op.drop_index(op.f('ix_copy_copy_approval_status'), table_name='copy')
    op.drop_index(op.f('ix_copy_needs_production'), table_name='copy')
    op.drop_index(op.f('ix_copy_copy_status'), table_name='copy')
    op.drop_index(op.f('ix_copy_production_order_id'), table_name='copy')
    op.drop_constraint('fk_copy_script_approver', 'copy', type_='foreignkey')
    op.drop_constraint('fk_copy_production_order', 'copy', type_='foreignkey')
    op.drop_column('copy', 'script_approved_by')
    op.drop_column('copy', 'script_approved_at')
    op.drop_column('copy', 'copy_approval_status')
    op.drop_column('copy', 'needs_production')
    op.drop_column('copy', 'copy_status')
    op.drop_column('copy', 'production_order_id')

    # Drop production_comments table
    op.drop_index(op.f('ix_production_comments_production_order_id'), table_name='production_comments')
    op.drop_table('production_comments')

    # Drop production_revisions table
    op.drop_index(op.f('ix_production_revisions_production_order_id'), table_name='production_revisions')
    op.drop_table('production_revisions')

    # Drop voice_talent_requests table
    op.drop_index(op.f('ix_voice_talent_requests_deadline'), table_name='voice_talent_requests')
    op.drop_index(op.f('ix_voice_talent_requests_status'), table_name='voice_talent_requests')
    op.drop_index(op.f('ix_voice_talent_requests_talent_type'), table_name='voice_talent_requests')
    op.drop_index(op.f('ix_voice_talent_requests_talent_user_id'), table_name='voice_talent_requests')
    op.drop_index(op.f('ix_voice_talent_requests_production_order_id'), table_name='voice_talent_requests')
    op.drop_table('voice_talent_requests')

    # Drop production_assignments table
    op.drop_index(op.f('ix_production_assignments_status'), table_name='production_assignments')
    op.drop_index(op.f('ix_production_assignments_assignment_type'), table_name='production_assignments')
    op.drop_index(op.f('ix_production_assignments_user_id'), table_name='production_assignments')
    op.drop_index(op.f('ix_production_assignments_production_order_id'), table_name='production_assignments')
    op.drop_table('production_assignments')

    # Drop production_orders table
    op.drop_index(op.f('ix_production_orders_deadline'), table_name='production_orders')
    op.drop_index(op.f('ix_production_orders_status'), table_name='production_orders')
    op.drop_index(op.f('ix_production_orders_advertiser_id'), table_name='production_orders')
    op.drop_index(op.f('ix_production_orders_campaign_id'), table_name='production_orders')
    op.drop_index(op.f('ix_production_orders_order_id'), table_name='production_orders')
    op.drop_index(op.f('ix_production_orders_copy_id'), table_name='production_orders')
    op.drop_index(op.f('ix_production_orders_po_number'), table_name='production_orders')
    op.drop_table('production_orders')

    # Restore users table constraint
    op.drop_constraint('users_role_check', 'users', type_='check')
    op.create_check_constraint(
        'users_role_check',
        'users',
        "role IN ('admin', 'producer', 'dj', 'sales')"
    )

