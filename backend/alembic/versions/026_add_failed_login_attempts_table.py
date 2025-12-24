"""Add failed login attempts table

Revision ID: 026
Revises: 025
Create Date: 2025-01-XX

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = '026'
down_revision = '025'
branch_labels = None
depends_on = None


def upgrade():
    # Create failed_login_attempts table
    op.create_table(
        'failed_login_attempts',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('username', sa.String(length=50), nullable=False),
        sa.Column('ip_address', sa.String(length=45), nullable=True),
        sa.Column('user_agent', sa.String(length=255), nullable=True),
        sa.Column('attempted_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.PrimaryKeyConstraint('id')
    )
    
    # Create indexes
    op.create_index(op.f('ix_failed_login_attempts_id'), 'failed_login_attempts', ['id'], unique=False)
    op.create_index(op.f('ix_failed_login_attempts_username'), 'failed_login_attempts', ['username'], unique=False)
    op.create_index('idx_username_attempted_at', 'failed_login_attempts', ['username', 'attempted_at'], unique=False)


def downgrade():
    # Drop indexes
    op.drop_index('idx_username_attempted_at', table_name='failed_login_attempts')
    op.drop_index(op.f('ix_failed_login_attempts_username'), table_name='failed_login_attempts')
    op.drop_index(op.f('ix_failed_login_attempts_id'), table_name='failed_login_attempts')
    
    # Drop table
    op.drop_table('failed_login_attempts')


