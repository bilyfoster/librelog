"""Initial migration

Revision ID: 0001
Revises: 
Create Date: 2024-01-01 00:00:00.000000

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = '0001'
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Create users table
    op.create_table('users',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('username', sa.String(length=50), nullable=False),
        sa.Column('password_hash', sa.String(length=255), nullable=False),
        sa.Column('role', sa.String(length=20), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('last_login', sa.DateTime(timezone=True), nullable=True),
        sa.CheckConstraint("role IN ('admin', 'producer', 'dj', 'sales')", name='users_role_check'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_users_id'), 'users', ['id'], unique=False)
    op.create_index(op.f('ix_users_username'), 'users', ['username'], unique=True)

    # Create tracks table
    op.create_table('tracks',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('title', sa.String(length=255), nullable=False),
        sa.Column('artist', sa.String(length=255), nullable=True),
        sa.Column('album', sa.String(length=255), nullable=True),
        sa.Column('type', sa.String(length=10), nullable=False),
        sa.Column('genre', sa.String(length=100), nullable=True),
        sa.Column('duration', sa.Integer(), nullable=True),
        sa.Column('filepath', sa.Text(), nullable=False),
        sa.Column('libretime_id', sa.String(length=50), nullable=True),
        sa.Column('last_played', sa.DateTime(timezone=True), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.CheckConstraint("type IN ('MUS', 'ADV', 'PSA', 'LIN', 'INT', 'PRO', 'BED')", name='tracks_type_check'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_tracks_id'), 'tracks', ['id'], unique=False)
    op.create_index(op.f('ix_tracks_title'), 'tracks', ['title'], unique=False)
    op.create_index(op.f('ix_tracks_artist'), 'tracks', ['artist'], unique=False)
    op.create_index(op.f('ix_tracks_type'), 'tracks', ['type'], unique=False)
    op.create_index(op.f('ix_tracks_genre'), 'tracks', ['genre'], unique=False)
    op.create_index(op.f('ix_tracks_libretime_id'), 'tracks', ['libretime_id'], unique=True)

    # Create campaigns table
    op.create_table('campaigns',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('advertiser', sa.String(length=255), nullable=False),
        sa.Column('start_date', sa.Date(), nullable=False),
        sa.Column('end_date', sa.Date(), nullable=False),
        sa.Column('priority', sa.Integer(), nullable=True),
        sa.Column('file_url', sa.Text(), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_campaigns_id'), 'campaigns', ['id'], unique=False)
    op.create_index(op.f('ix_campaigns_advertiser'), 'campaigns', ['advertiser'], unique=False)
    op.create_index(op.f('ix_campaigns_start_date'), 'campaigns', ['start_date'], unique=False)
    op.create_index(op.f('ix_campaigns_end_date'), 'campaigns', ['end_date'], unique=False)
    op.create_index(op.f('ix_campaigns_priority'), 'campaigns', ['priority'], unique=False)
    op.create_index(op.f('ix_campaigns_active'), 'campaigns', ['active'], unique=False)

    # Create clock_templates table
    op.create_table('clock_templates',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('name', sa.String(length=100), nullable=False),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('json_layout', postgresql.JSONB(astext_type=sa.Text()), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_clock_templates_id'), 'clock_templates', ['id'], unique=False)
    op.create_index(op.f('ix_clock_templates_name'), 'clock_templates', ['name'], unique=False)

    # Create daily_logs table
    op.create_table('daily_logs',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('date', sa.Date(), nullable=False),
        sa.Column('generated_by', sa.Integer(), nullable=False),
        sa.Column('json_data', postgresql.JSONB(astext_type=sa.Text()), nullable=False),
        sa.Column('published', sa.Boolean(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['generated_by'], ['users.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_daily_logs_id'), 'daily_logs', ['id'], unique=False)
    op.create_index(op.f('ix_daily_logs_date'), 'daily_logs', ['date'], unique=False)
    op.create_index(op.f('ix_daily_logs_published'), 'daily_logs', ['published'], unique=False)

    # Create voice_tracks table
    op.create_table('voice_tracks',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('show_name', sa.String(length=255), nullable=True),
        sa.Column('file_url', sa.Text(), nullable=False),
        sa.Column('scheduled_time', sa.DateTime(timezone=True), nullable=True),
        sa.Column('uploaded_by', sa.Integer(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['uploaded_by'], ['users.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_voice_tracks_id'), 'voice_tracks', ['id'], unique=False)
    op.create_index(op.f('ix_voice_tracks_show_name'), 'voice_tracks', ['show_name'], unique=False)
    op.create_index(op.f('ix_voice_tracks_scheduled_time'), 'voice_tracks', ['scheduled_time'], unique=False)

    # Create playback_history table
    op.create_table('playback_history',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('track_id', sa.Integer(), nullable=False),
        sa.Column('log_id', sa.Integer(), nullable=False),
        sa.Column('played_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('duration_played', sa.Integer(), nullable=True),
        sa.ForeignKeyConstraint(['log_id'], ['daily_logs.id'], ),
        sa.ForeignKeyConstraint(['track_id'], ['tracks.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_playback_history_id'), 'playback_history', ['id'], unique=False)
    op.create_index(op.f('ix_playback_history_played_at'), 'playback_history', ['played_at'], unique=False)


def downgrade() -> None:
    op.drop_index(op.f('ix_playback_history_played_at'), table_name='playback_history')
    op.drop_index(op.f('ix_playback_history_id'), table_name='playback_history')
    op.drop_table('playback_history')
    op.drop_index(op.f('ix_voice_tracks_scheduled_time'), table_name='voice_tracks')
    op.drop_index(op.f('ix_voice_tracks_show_name'), table_name='voice_tracks')
    op.drop_index(op.f('ix_voice_tracks_id'), table_name='voice_tracks')
    op.drop_table('voice_tracks')
    op.drop_index(op.f('ix_daily_logs_published'), table_name='daily_logs')
    op.drop_index(op.f('ix_daily_logs_date'), table_name='daily_logs')
    op.drop_index(op.f('ix_daily_logs_id'), table_name='daily_logs')
    op.drop_table('daily_logs')
    op.drop_index(op.f('ix_clock_templates_name'), table_name='clock_templates')
    op.drop_index(op.f('ix_clock_templates_id'), table_name='clock_templates')
    op.drop_table('clock_templates')
    op.drop_index(op.f('ix_campaigns_active'), table_name='campaigns')
    op.drop_index(op.f('ix_campaigns_priority'), table_name='campaigns')
    op.drop_index(op.f('ix_campaigns_end_date'), table_name='campaigns')
    op.drop_index(op.f('ix_campaigns_start_date'), table_name='campaigns')
    op.drop_index(op.f('ix_campaigns_advertiser'), table_name='campaigns')
    op.drop_index(op.f('ix_campaigns_id'), table_name='campaigns')
    op.drop_table('campaigns')
    op.drop_index(op.f('ix_tracks_libretime_id'), table_name='tracks')
    op.drop_index(op.f('ix_tracks_genre'), table_name='tracks')
    op.drop_index(op.f('ix_tracks_type'), table_name='tracks')
    op.drop_index(op.f('ix_tracks_artist'), table_name='tracks')
    op.drop_index(op.f('ix_tracks_title'), table_name='tracks')
    op.drop_index(op.f('ix_tracks_id'), table_name='tracks')
    op.drop_table('tracks')
    op.drop_index(op.f('ix_users_username'), table_name='users')
    op.drop_index(op.f('ix_users_id'), table_name='users')
    op.drop_table('users')
