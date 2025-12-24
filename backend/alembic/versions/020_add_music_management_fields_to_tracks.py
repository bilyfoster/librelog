"""Add music management fields to tracks

Revision ID: 020
Revises: 019
Create Date: 2025-11-20

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = '020'
down_revision = '019'  # 019 was manually applied, using 018 as base
branch_labels = None
depends_on = None


def upgrade():
    # Add BPM field
    op.add_column('tracks', sa.Column('bpm', sa.Integer(), nullable=True))
    op.create_index(op.f('ix_tracks_bpm'), 'tracks', ['bpm'], unique=False)
    
    # Add daypart_eligible field (JSONB array of daypart IDs)
    op.add_column('tracks', sa.Column('daypart_eligible', postgresql.JSONB(astext_type=sa.Text()), nullable=True))
    
    # Add is_new_release flag
    op.add_column('tracks', sa.Column('is_new_release', sa.Boolean(), nullable=True, server_default='false'))
    op.create_index(op.f('ix_tracks_is_new_release'), 'tracks', ['is_new_release'], unique=False)
    
    # Add allow_back_to_back flag
    op.add_column('tracks', sa.Column('allow_back_to_back', sa.Boolean(), nullable=True, server_default='false'))


def downgrade():
    # Remove indexes first
    op.drop_index(op.f('ix_tracks_is_new_release'), table_name='tracks')
    op.drop_index(op.f('ix_tracks_bpm'), table_name='tracks')
    
    # Remove columns
    op.drop_column('tracks', 'allow_back_to_back')
    op.drop_column('tracks', 'is_new_release')
    op.drop_column('tracks', 'daypart_eligible')
    op.drop_column('tracks', 'bpm')

