"""Add standardized_name to voice tracks and slots for fallback lookup

Revision ID: 019
Revises: 018
Create Date: 2025-01-21 12:00:00.000000

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = '019'
down_revision = '018'
branch_labels = None
depends_on = None


def upgrade():
    # Add standardized_name to voice_track_slots
    op.add_column('voice_track_slots', sa.Column('standardized_name', sa.String(length=50), nullable=True))
    op.create_index(op.f('ix_voice_track_slots_standardized_name'), 'voice_track_slots', ['standardized_name'], unique=False)
    
    # Add standardized_name and recorded_date to voice_tracks
    op.add_column('voice_tracks', sa.Column('standardized_name', sa.String(length=50), nullable=True))
    op.add_column('voice_tracks', sa.Column('recorded_date', sa.DateTime(timezone=True), nullable=True))
    op.create_index(op.f('ix_voice_tracks_standardized_name'), 'voice_tracks', ['standardized_name'], unique=False)
    op.create_index(op.f('ix_voice_tracks_recorded_date'), 'voice_tracks', ['recorded_date'], unique=False)


def downgrade():
    # Drop indexes
    op.drop_index(op.f('ix_voice_tracks_recorded_date'), table_name='voice_tracks')
    op.drop_index(op.f('ix_voice_tracks_standardized_name'), table_name='voice_tracks')
    op.drop_index(op.f('ix_voice_track_slots_standardized_name'), table_name='voice_track_slots')
    
    # Drop columns
    op.drop_column('voice_tracks', 'recorded_date')
    op.drop_column('voice_tracks', 'standardized_name')
    op.drop_column('voice_track_slots', 'standardized_name')

