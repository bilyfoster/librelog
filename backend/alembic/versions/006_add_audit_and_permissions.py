"""Add audit and permissions tables

Revision ID: 006
Revises: 005
Create Date: 2024-01-15 00:00:00.000000

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = '006'
down_revision = '005'
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Extend users table
    op.add_column('users', sa.Column('last_activity', sa.DateTime(timezone=True), nullable=True))
    op.add_column('users', sa.Column('permissions', postgresql.JSONB(astext_type=sa.Text()), nullable=True))
    
    # Create audit_logs table
    op.create_table('audit_logs',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('user_id', sa.Integer(), nullable=False),
        sa.Column('action', sa.String(length=100), nullable=False),
        sa.Column('resource_type', sa.String(length=50), nullable=False),
        sa.Column('resource_id', sa.Integer(), nullable=True),
        sa.Column('changes', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('ip_address', sa.String(length=45), nullable=True),
        sa.Column('user_agent', sa.String(length=500), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_audit_logs_id'), 'audit_logs', ['id'], unique=False)
    op.create_index(op.f('ix_audit_logs_user_id'), 'audit_logs', ['user_id'], unique=False)
    op.create_index(op.f('ix_audit_logs_action'), 'audit_logs', ['action'], unique=False)
    op.create_index(op.f('ix_audit_logs_resource_type'), 'audit_logs', ['resource_type'], unique=False)
    op.create_index(op.f('ix_audit_logs_resource_id'), 'audit_logs', ['resource_id'], unique=False)
    op.create_index(op.f('ix_audit_logs_created_at'), 'audit_logs', ['created_at'], unique=False)
    
    # Create log_revisions table
    op.create_table('log_revisions',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('log_id', sa.Integer(), nullable=False),
        sa.Column('revision_number', sa.Integer(), nullable=False),
        sa.Column('json_data', postgresql.JSONB(astext_type=sa.Text()), nullable=False),
        sa.Column('changed_by', sa.Integer(), nullable=False),
        sa.Column('change_summary', sa.Text(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['changed_by'], ['users.id'], ),
        sa.ForeignKeyConstraint(['log_id'], ['daily_logs.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_log_revisions_id'), 'log_revisions', ['id'], unique=False)
    op.create_index(op.f('ix_log_revisions_log_id'), 'log_revisions', ['log_id'], unique=False)
    op.create_index(op.f('ix_log_revisions_revision_number'), 'log_revisions', ['revision_number'], unique=False)
    op.create_index(op.f('ix_log_revisions_created_at'), 'log_revisions', ['created_at'], unique=False)


def downgrade() -> None:
    # Drop log_revisions table
    op.drop_index(op.f('ix_log_revisions_created_at'), table_name='log_revisions')
    op.drop_index(op.f('ix_log_revisions_revision_number'), table_name='log_revisions')
    op.drop_index(op.f('ix_log_revisions_log_id'), table_name='log_revisions')
    op.drop_index(op.f('ix_log_revisions_id'), table_name='log_revisions')
    op.drop_table('log_revisions')
    
    # Drop audit_logs table
    op.drop_index(op.f('ix_audit_logs_created_at'), table_name='audit_logs')
    op.drop_index(op.f('ix_audit_logs_resource_id'), table_name='audit_logs')
    op.drop_index(op.f('ix_audit_logs_resource_type'), table_name='audit_logs')
    op.drop_index(op.f('ix_audit_logs_action'), table_name='audit_logs')
    op.drop_index(op.f('ix_audit_logs_user_id'), table_name='audit_logs')
    op.drop_index(op.f('ix_audit_logs_id'), table_name='audit_logs')
    op.drop_table('audit_logs')
    
    # Remove extended fields from users
    op.drop_column('users', 'permissions')
    op.drop_column('users', 'last_activity')

