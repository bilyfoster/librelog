"""Rename metadata columns to meta_data to avoid SQLAlchemy conflict

Revision ID: 012
Revises: 011
Create Date: 2024-01-22 00:00:00.000000

"""
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = '012'
down_revision = '011'
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Rename metadata column to meta_data in traffic_logs table
    op.alter_column('traffic_logs', 'metadata', new_column_name='meta_data', existing_type=sa.dialects.postgresql.JSONB)
    
    # Rename metadata column to meta_data in notifications table
    op.alter_column('notifications', 'metadata', new_column_name='meta_data', existing_type=sa.dialects.postgresql.JSONB)


def downgrade() -> None:
    # Rename meta_data back to metadata in traffic_logs table
    op.alter_column('traffic_logs', 'meta_data', new_column_name='metadata', existing_type=sa.dialects.postgresql.JSONB)
    
    # Rename meta_data back to metadata in notifications table
    op.alter_column('notifications', 'meta_data', new_column_name='metadata', existing_type=sa.dialects.postgresql.JSONB)

