"""Add webhooks and notifications tables

Revision ID: 009
Revises: 008
Create Date: 2024-01-20 00:00:00.000000

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = '009'
down_revision = '008'
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Create enum types
    op.execute("CREATE TYPE webhooktype AS ENUM ('DISCORD', 'SLACK', 'CUSTOM')")
    op.execute("CREATE TYPE webhookevent AS ENUM ('LOG_PUBLISHED', 'LOG_LOCKED', 'ORDER_CREATED', 'ORDER_APPROVED', 'INVOICE_CREATED', 'INVOICE_PAID', 'CAMPAIGN_CREATED', 'CAMPAIGN_UPDATED', 'COPY_EXPIRING', 'SPOT_CONFLICT', 'USER_ACTIVITY')")
    op.execute("CREATE TYPE notificationtype AS ENUM ('EMAIL', 'IN_APP', 'BOTH')")
    op.execute("CREATE TYPE notificationstatus AS ENUM ('PENDING', 'SENT', 'FAILED', 'READ')")
    
    # Create webhooks table
    op.create_table('webhooks',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('name', sa.String(length=255), nullable=False),
        sa.Column('webhook_type', postgresql.ENUM('DISCORD', 'SLACK', 'CUSTOM', name='webhooktype'), nullable=False),
        sa.Column('url', sa.Text(), nullable=False),
        sa.Column('events', postgresql.JSONB(astext_type=sa.Text()), nullable=False),
        sa.Column('secret', sa.String(length=255), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('last_triggered_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('last_error', sa.Text(), nullable=True),
        sa.Column('error_count', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('headers', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('created_by', sa.Integer(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.ForeignKeyConstraint(['created_by'], ['users.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_webhooks_id'), 'webhooks', ['id'], unique=False)
    op.create_index(op.f('ix_webhooks_webhook_type'), 'webhooks', ['webhook_type'], unique=False)
    op.create_index(op.f('ix_webhooks_active'), 'webhooks', ['active'], unique=False)
    
    # Create notifications table
    op.create_table('notifications',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('user_id', sa.Integer(), nullable=False),
        sa.Column('notification_type', postgresql.ENUM('EMAIL', 'IN_APP', 'BOTH', name='notificationtype'), nullable=False, server_default='IN_APP'),
        sa.Column('status', postgresql.ENUM('PENDING', 'SENT', 'FAILED', 'READ', name='notificationstatus'), nullable=False, server_default='PENDING'),
        sa.Column('subject', sa.String(length=255), nullable=True),
        sa.Column('message', sa.Text(), nullable=False),
        sa.Column('metadata', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('read_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('sent_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('error_message', sa.Text(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_notifications_id'), 'notifications', ['id'], unique=False)
    op.create_index(op.f('ix_notifications_user_id'), 'notifications', ['user_id'], unique=False)
    op.create_index(op.f('ix_notifications_status'), 'notifications', ['status'], unique=False)
    op.create_index(op.f('ix_notifications_created_at'), 'notifications', ['created_at'], unique=False)


def downgrade() -> None:
    op.drop_index(op.f('ix_notifications_created_at'), table_name='notifications')
    op.drop_index(op.f('ix_notifications_status'), table_name='notifications')
    op.drop_index(op.f('ix_notifications_user_id'), table_name='notifications')
    op.drop_index(op.f('ix_notifications_id'), table_name='notifications')
    op.drop_table('notifications')
    
    op.drop_index(op.f('ix_webhooks_active'), table_name='webhooks')
    op.drop_index(op.f('ix_webhooks_webhook_type'), table_name='webhooks')
    op.drop_index(op.f('ix_webhooks_id'), table_name='webhooks')
    op.drop_table('webhooks')
    
    op.execute("DROP TYPE notificationstatus")
    op.execute("DROP TYPE notificationtype")
    op.execute("DROP TYPE webhookevent")
    op.execute("DROP TYPE webhooktype")

