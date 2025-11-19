"""Extend voice track model for voice tracking features

Revision ID: 018
Revises: 017
Create Date: 2025-01-21 10:00:00.000000

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = '018'
down_revision = '017'
branch_labels = None
depends_on = None


def upgrade():
    # Add new columns to voice_tracks table
    op.add_column('voice_tracks', sa.Column('break_id', sa.Integer(), nullable=True))
    op.add_column('voice_tracks', sa.Column('playlist_id', sa.Integer(), nullable=True))
    op.add_column('voice_tracks', sa.Column('break_type', sa.String(length=20), nullable=True))
    op.add_column('voice_tracks', sa.Column('slot_position', sa.String(length=10), nullable=True))
    op.add_column('voice_tracks', sa.Column('ramp_time', sa.Numeric(precision=5, scale=2), nullable=True))
    op.add_column('voice_tracks', sa.Column('back_time', sa.Numeric(precision=5, scale=2), nullable=True))
    op.add_column('voice_tracks', sa.Column('take_number', sa.Integer(), server_default='1', nullable=True))
    op.add_column('voice_tracks', sa.Column('is_final', sa.Boolean(), server_default='false', nullable=True))
    op.add_column('voice_tracks', sa.Column('status', sa.String(length=20), server_default='draft', nullable=True))
    op.add_column('voice_tracks', sa.Column('ducking_threshold', sa.Numeric(precision=5, scale=2), server_default='-18.0', nullable=True))
    op.add_column('voice_tracks', sa.Column('raw_file_url', sa.Text(), nullable=True))
    op.add_column('voice_tracks', sa.Column('mixed_file_url', sa.Text(), nullable=True))
    op.add_column('voice_tracks', sa.Column('track_metadata', postgresql.JSONB(astext_type=sa.Text()), nullable=True))
    op.add_column('voice_tracks', sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True))
    
    # Create indexes
    op.create_index(op.f('ix_voice_tracks_break_id'), 'voice_tracks', ['break_id'], unique=False)
    op.create_index(op.f('ix_voice_tracks_playlist_id'), 'voice_tracks', ['playlist_id'], unique=False)
    op.create_index(op.f('ix_voice_tracks_is_final'), 'voice_tracks', ['is_final'], unique=False)
    op.create_index(op.f('ix_voice_tracks_status'), 'voice_tracks', ['status'], unique=False)
    
    # Create voice_track_slots table
    op.create_table(
        'voice_track_slots',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('log_id', sa.Integer(), nullable=False),
        sa.Column('hour', sa.Integer(), nullable=False),
        sa.Column('break_position', sa.String(length=10), nullable=True),
        sa.Column('assigned_dj_id', sa.Integer(), nullable=True),
        sa.Column('voice_track_id', sa.Integer(), nullable=True),
        sa.Column('previous_track_id', sa.Integer(), nullable=True),
        sa.Column('next_track_id', sa.Integer(), nullable=True),
        sa.Column('ramp_time', sa.Numeric(precision=5, scale=2), nullable=True),
        sa.Column('status', sa.String(length=20), server_default='pending', nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['assigned_dj_id'], ['users.id'], ),
        sa.ForeignKeyConstraint(['log_id'], ['daily_logs.id'], ),
        sa.ForeignKeyConstraint(['next_track_id'], ['tracks.id'], ),
        sa.ForeignKeyConstraint(['previous_track_id'], ['tracks.id'], ),
        sa.ForeignKeyConstraint(['voice_track_id'], ['voice_tracks.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_voice_track_slots_log_id'), 'voice_track_slots', ['log_id'], unique=False)
    op.create_index(op.f('ix_voice_track_slots_hour'), 'voice_track_slots', ['hour'], unique=False)
    op.create_index(op.f('ix_voice_track_slots_assigned_dj_id'), 'voice_track_slots', ['assigned_dj_id'], unique=False)
    op.create_index(op.f('ix_voice_track_slots_voice_track_id'), 'voice_track_slots', ['voice_track_id'], unique=False)
    op.create_index(op.f('ix_voice_track_slots_status'), 'voice_track_slots', ['status'], unique=False)
    
    # Create voice_track_audit table
    op.create_table(
        'voice_track_audit',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('voice_track_id', sa.Integer(), nullable=False),
        sa.Column('action', sa.String(length=50), nullable=False),
        sa.Column('user_id', sa.Integer(), nullable=False),
        sa.Column('timestamp', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('audit_metadata', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ),
        sa.ForeignKeyConstraint(['voice_track_id'], ['voice_tracks.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_voice_track_audit_voice_track_id'), 'voice_track_audit', ['voice_track_id'], unique=False)
    op.create_index(op.f('ix_voice_track_audit_user_id'), 'voice_track_audit', ['user_id'], unique=False)
    op.create_index(op.f('ix_voice_track_audit_timestamp'), 'voice_track_audit', ['timestamp'], unique=False)


def downgrade():
    # Drop indexes
    op.drop_index(op.f('ix_voice_track_audit_timestamp'), table_name='voice_track_audit')
    op.drop_index(op.f('ix_voice_track_audit_user_id'), table_name='voice_track_audit')
    op.drop_index(op.f('ix_voice_track_audit_voice_track_id'), table_name='voice_track_audit')
    op.drop_index(op.f('ix_voice_track_slots_status'), table_name='voice_track_slots')
    op.drop_index(op.f('ix_voice_track_slots_voice_track_id'), table_name='voice_track_slots')
    op.drop_index(op.f('ix_voice_track_slots_assigned_dj_id'), table_name='voice_track_slots')
    op.drop_index(op.f('ix_voice_track_slots_hour'), table_name='voice_track_slots')
    op.drop_index(op.f('ix_voice_track_slots_log_id'), table_name='voice_track_slots')
    op.drop_index(op.f('ix_voice_tracks_status'), table_name='voice_tracks')
    op.drop_index(op.f('ix_voice_tracks_is_final'), table_name='voice_tracks')
    op.drop_index(op.f('ix_voice_tracks_playlist_id'), table_name='voice_tracks')
    op.drop_index(op.f('ix_voice_tracks_break_id'), table_name='voice_tracks')
    
    # Drop tables
    op.drop_table('voice_track_audit')
    op.drop_table('voice_track_slots')
    
    # Drop columns from voice_tracks
    op.drop_column('voice_tracks', 'updated_at')
    op.drop_column('voice_tracks', 'track_metadata')
    op.drop_column('voice_tracks', 'mixed_file_url')
    op.drop_column('voice_tracks', 'raw_file_url')
    op.drop_column('voice_tracks', 'ducking_threshold')
    op.drop_column('voice_tracks', 'status')
    op.drop_column('voice_tracks', 'is_final')
    op.drop_column('voice_tracks', 'take_number')
    op.drop_column('voice_tracks', 'back_time')
    op.drop_column('voice_tracks', 'ramp_time')
    op.drop_column('voice_tracks', 'slot_position')
    op.drop_column('voice_tracks', 'break_type')
    op.drop_column('voice_tracks', 'playlist_id')
    op.drop_column('voice_tracks', 'break_id')

