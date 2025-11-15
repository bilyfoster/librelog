"""Add audio management tables

Revision ID: 015
Revises: 014
Create Date: 2025-11-15 05:50:00.000000

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = '015'
down_revision = '014'
branch_labels = None
depends_on = None


def upgrade():
    # Create audio_cuts table
    op.create_table(
        'audio_cuts',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('copy_id', sa.Integer(), nullable=False),
        sa.Column('cut_id', sa.String(length=50), nullable=False),
        sa.Column('cut_name', sa.String(length=255), nullable=True),
        sa.Column('audio_file_path', sa.Text(), nullable=True),
        sa.Column('audio_file_url', sa.Text(), nullable=True),
        sa.Column('file_checksum', sa.String(length=64), nullable=True),
        sa.Column('version', sa.Integer(), nullable=False, server_default='1'),
        sa.Column('rotation_weight', sa.Float(), nullable=True, server_default='1.0'),
        sa.Column('daypart_restrictions', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('program_associations', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('expires_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=True, server_default='true'),
        sa.Column('qc_results', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('notes', sa.Text(), nullable=True),
        sa.Column('tags', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('created_by', sa.Integer(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['copy_id'], ['copy.id'], ),
        sa.ForeignKeyConstraint(['created_by'], ['users.id'], ),
        sa.PrimaryKeyConstraint('id'),
        sa.CheckConstraint('rotation_weight >= 0', name='check_rotation_weight_positive')
    )
    op.create_index(op.f('ix_audio_cuts_copy_id'), 'audio_cuts', ['copy_id'], unique=False)
    op.create_index(op.f('ix_audio_cuts_cut_id'), 'audio_cuts', ['cut_id'], unique=False)
    op.create_index(op.f('ix_audio_cuts_expires_at'), 'audio_cuts', ['expires_at'], unique=False)
    op.create_index(op.f('ix_audio_cuts_active'), 'audio_cuts', ['active'], unique=False)

    # Create audio_versions table
    op.create_table(
        'audio_versions',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('cut_id', sa.Integer(), nullable=False),
        sa.Column('version_number', sa.Integer(), nullable=False),
        sa.Column('audio_file_path', sa.Text(), nullable=True),
        sa.Column('audio_file_url', sa.Text(), nullable=True),
        sa.Column('file_checksum', sa.String(length=64), nullable=True),
        sa.Column('version_notes', sa.Text(), nullable=True),
        sa.Column('changed_by', sa.Integer(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['cut_id'], ['audio_cuts.id'], ),
        sa.ForeignKeyConstraint(['changed_by'], ['users.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_audio_versions_cut_id'), 'audio_versions', ['cut_id'], unique=False)

    # Create live_reads table
    op.create_table(
        'live_reads',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('copy_id', sa.Integer(), nullable=True),
        sa.Column('order_id', sa.Integer(), nullable=True),
        sa.Column('advertiser_id', sa.Integer(), nullable=True),
        sa.Column('script_text', sa.Text(), nullable=False),
        sa.Column('script_title', sa.String(length=255), nullable=True),
        sa.Column('scheduled_time', sa.DateTime(timezone=True), nullable=True),
        sa.Column('scheduled_date', sa.DateTime(timezone=True), nullable=True),
        sa.Column('performed_time', sa.DateTime(timezone=True), nullable=True),
        sa.Column('performed_by', sa.Integer(), nullable=True),
        sa.Column('confirmed', sa.Boolean(), nullable=True, server_default='false'),
        sa.Column('proof_of_performance', sa.Text(), nullable=True),
        sa.Column('confirmation_timestamp', sa.DateTime(timezone=True), nullable=True),
        sa.Column('status', sa.String(length=50), nullable=True, server_default='scheduled'),
        sa.Column('makegood_required', sa.Boolean(), nullable=True, server_default='false'),
        sa.Column('makegood_id', sa.Integer(), nullable=True),
        sa.Column('notes', sa.Text(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['advertiser_id'], ['advertisers.id'], ),
        sa.ForeignKeyConstraint(['copy_id'], ['copy.id'], ),
        sa.ForeignKeyConstraint(['makegood_id'], ['makegoods.id'], ),
        sa.ForeignKeyConstraint(['order_id'], ['orders.id'], ),
        sa.ForeignKeyConstraint(['performed_by'], ['users.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_live_reads_copy_id'), 'live_reads', ['copy_id'], unique=False)
    op.create_index(op.f('ix_live_reads_order_id'), 'live_reads', ['order_id'], unique=False)
    op.create_index(op.f('ix_live_reads_advertiser_id'), 'live_reads', ['advertiser_id'], unique=False)
    op.create_index(op.f('ix_live_reads_scheduled_time'), 'live_reads', ['scheduled_time'], unique=False)
    op.create_index(op.f('ix_live_reads_scheduled_date'), 'live_reads', ['scheduled_date'], unique=False)
    op.create_index(op.f('ix_live_reads_performed_time'), 'live_reads', ['performed_time'], unique=False)
    op.create_index(op.f('ix_live_reads_confirmed'), 'live_reads', ['confirmed'], unique=False)
    op.create_index(op.f('ix_live_reads_status'), 'live_reads', ['status'], unique=False)
    op.create_index(op.f('ix_live_reads_makegood_required'), 'live_reads', ['makegood_required'], unique=False)

    # Create political_records table
    op.create_table(
        'political_records',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('copy_id', sa.Integer(), nullable=True),
        sa.Column('order_id', sa.Integer(), nullable=True),
        sa.Column('advertiser_id', sa.Integer(), nullable=True),
        sa.Column('advertiser_category', sa.String(length=100), nullable=False),
        sa.Column('sponsor_name', sa.String(length=255), nullable=False),
        sa.Column('sponsor_id', sa.String(length=100), nullable=True),
        sa.Column('office_sought', sa.String(length=255), nullable=True),
        sa.Column('disclaimers_required', sa.Text(), nullable=True),
        sa.Column('disclaimers_included', sa.Boolean(), nullable=True, server_default='false'),
        sa.Column('compliance_status', sa.String(length=50), nullable=True, server_default='pending'),
        sa.Column('no_substitution_allowed', sa.Boolean(), nullable=True, server_default='true'),
        sa.Column('archived', sa.Boolean(), nullable=True, server_default='false'),
        sa.Column('archive_date', sa.DateTime(timezone=True), nullable=True),
        sa.Column('archive_location', sa.Text(), nullable=True),
        sa.Column('political_window_start', sa.DateTime(timezone=True), nullable=True),
        sa.Column('political_window_end', sa.DateTime(timezone=True), nullable=True),
        sa.Column('no_preemptions', sa.Boolean(), nullable=True, server_default='true'),
        sa.Column('priority_scheduling', sa.Boolean(), nullable=True, server_default='true'),
        sa.Column('notes', sa.Text(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['advertiser_id'], ['advertisers.id'], ),
        sa.ForeignKeyConstraint(['copy_id'], ['copy.id'], ),
        sa.ForeignKeyConstraint(['order_id'], ['orders.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_political_records_copy_id'), 'political_records', ['copy_id'], unique=False)
    op.create_index(op.f('ix_political_records_order_id'), 'political_records', ['order_id'], unique=False)
    op.create_index(op.f('ix_political_records_advertiser_id'), 'political_records', ['advertiser_id'], unique=False)
    op.create_index(op.f('ix_political_records_sponsor_name'), 'political_records', ['sponsor_name'], unique=False)
    op.create_index(op.f('ix_political_records_compliance_status'), 'political_records', ['compliance_status'], unique=False)
    op.create_index(op.f('ix_political_records_archived'), 'political_records', ['archived'], unique=False)

    # Create audio_deliveries table
    op.create_table(
        'audio_deliveries',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('cut_id', sa.Integer(), nullable=False),
        sa.Column('copy_id', sa.Integer(), nullable=True),
        sa.Column('delivery_method', sa.String(length=20), nullable=False),
        sa.Column('target_server', sa.String(length=255), nullable=False),
        sa.Column('target_path', sa.Text(), nullable=True),
        sa.Column('status', sa.String(length=20), nullable=True, server_default='pending'),
        sa.Column('retry_count', sa.Integer(), nullable=True, server_default='0'),
        sa.Column('max_retries', sa.Integer(), nullable=True, server_default='3'),
        sa.Column('source_checksum', sa.String(length=64), nullable=True),
        sa.Column('delivered_checksum', sa.String(length=64), nullable=True),
        sa.Column('checksum_verified', sa.Boolean(), nullable=True, server_default='false'),
        sa.Column('delivery_started_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('delivery_completed_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('last_retry_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('error_message', sa.Text(), nullable=True),
        sa.Column('error_details', sa.Text(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['cut_id'], ['audio_cuts.id'], ),
        sa.ForeignKeyConstraint(['copy_id'], ['copy.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_audio_deliveries_cut_id'), 'audio_deliveries', ['cut_id'], unique=False)
    op.create_index(op.f('ix_audio_deliveries_copy_id'), 'audio_deliveries', ['copy_id'], unique=False)
    op.create_index(op.f('ix_audio_deliveries_status'), 'audio_deliveries', ['status'], unique=False)

    # Create audio_qc_results table
    op.create_table(
        'audio_qc_results',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('cut_id', sa.Integer(), nullable=False),
        sa.Column('version_id', sa.Integer(), nullable=True),
        sa.Column('qc_data', postgresql.JSONB(astext_type=sa.Text()), nullable=False),
        sa.Column('duration_seconds', sa.Float(), nullable=True),
        sa.Column('sample_rate', sa.Integer(), nullable=True),
        sa.Column('bitrate', sa.Integer(), nullable=True),
        sa.Column('channels', sa.Integer(), nullable=True),
        sa.Column('silence_at_head', sa.Float(), nullable=True),
        sa.Column('silence_at_tail', sa.Float(), nullable=True),
        sa.Column('silence_detected', sa.Boolean(), nullable=True, server_default='false'),
        sa.Column('peak_level', sa.Float(), nullable=True),
        sa.Column('rms_level', sa.Float(), nullable=True),
        sa.Column('lufs_level', sa.Float(), nullable=True),
        sa.Column('normalization_applied', sa.Boolean(), nullable=True, server_default='false'),
        sa.Column('normalization_method', sa.String(length=50), nullable=True),
        sa.Column('clipping_detected', sa.Boolean(), nullable=True, server_default='false'),
        sa.Column('clipping_samples', sa.Integer(), nullable=True),
        sa.Column('volume_threshold_passed', sa.Boolean(), nullable=True, server_default='true'),
        sa.Column('minimum_volume', sa.Float(), nullable=True),
        sa.Column('format_valid', sa.Boolean(), nullable=True, server_default='true'),
        sa.Column('format_type', sa.String(length=20), nullable=True),
        sa.Column('format_errors', sa.Text(), nullable=True),
        sa.Column('file_corrupted', sa.Boolean(), nullable=True, server_default='false'),
        sa.Column('corruption_details', sa.Text(), nullable=True),
        sa.Column('qc_passed', sa.Boolean(), nullable=True, server_default='false'),
        sa.Column('qc_warnings', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('qc_errors', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('overridden', sa.Boolean(), nullable=True, server_default='false'),
        sa.Column('overridden_by', sa.Integer(), nullable=True),
        sa.Column('override_reason', sa.Text(), nullable=True),
        sa.Column('override_timestamp', sa.DateTime(timezone=True), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['cut_id'], ['audio_cuts.id'], ),
        sa.ForeignKeyConstraint(['version_id'], ['audio_versions.id'], ),
        sa.ForeignKeyConstraint(['overridden_by'], ['users.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_audio_qc_results_cut_id'), 'audio_qc_results', ['cut_id'], unique=False)
    op.create_index(op.f('ix_audio_qc_results_version_id'), 'audio_qc_results', ['version_id'], unique=False)
    op.create_index(op.f('ix_audio_qc_results_silence_detected'), 'audio_qc_results', ['silence_detected'], unique=False)
    op.create_index(op.f('ix_audio_qc_results_clipping_detected'), 'audio_qc_results', ['clipping_detected'], unique=False)
    op.create_index(op.f('ix_audio_qc_results_format_valid'), 'audio_qc_results', ['format_valid'], unique=False)
    op.create_index(op.f('ix_audio_qc_results_file_corrupted'), 'audio_qc_results', ['file_corrupted'], unique=False)
    op.create_index(op.f('ix_audio_qc_results_qc_passed'), 'audio_qc_results', ['qc_passed'], unique=False)

    # Add new columns to copy table
    op.add_column('copy', sa.Column('cut_count', sa.Integer(), nullable=True, server_default='0'))
    op.add_column('copy', sa.Column('rotation_mode', sa.String(length=50), nullable=True))
    op.add_column('copy', sa.Column('political_flag', sa.Boolean(), nullable=True, server_default='false'))
    op.add_column('copy', sa.Column('live_read_enabled', sa.Boolean(), nullable=True, server_default='false'))
    op.add_column('copy', sa.Column('copy_instructions', sa.Text(), nullable=True))
    op.add_column('copy', sa.Column('instruction_attachments', sa.Text(), nullable=True))
    op.create_index(op.f('ix_copy_political_flag'), 'copy', ['political_flag'], unique=False)
    op.create_index(op.f('ix_copy_live_read_enabled'), 'copy', ['live_read_enabled'], unique=False)

    # Add new columns to rotation_rules table
    op.add_column('rotation_rules', sa.Column('cut_specific', sa.Boolean(), nullable=True, server_default='false'))
    op.add_column('rotation_rules', sa.Column('cut_weights', postgresql.JSONB(astext_type=sa.Text()), nullable=True))
    op.add_column('rotation_rules', sa.Column('program_specific', sa.Boolean(), nullable=True, server_default='false'))


def downgrade():
    # Remove columns from rotation_rules
    op.drop_column('rotation_rules', 'program_specific')
    op.drop_column('rotation_rules', 'cut_weights')
    op.drop_column('rotation_rules', 'cut_specific')

    # Remove columns from copy table
    op.drop_index(op.f('ix_copy_live_read_enabled'), table_name='copy')
    op.drop_index(op.f('ix_copy_political_flag'), table_name='copy')
    op.drop_column('copy', 'instruction_attachments')
    op.drop_column('copy', 'copy_instructions')
    op.drop_column('copy', 'live_read_enabled')
    op.drop_column('copy', 'political_flag')
    op.drop_column('copy', 'rotation_mode')
    op.drop_column('copy', 'cut_count')

    # Drop audio_qc_results table
    op.drop_index(op.f('ix_audio_qc_results_qc_passed'), table_name='audio_qc_results')
    op.drop_index(op.f('ix_audio_qc_results_file_corrupted'), table_name='audio_qc_results')
    op.drop_index(op.f('ix_audio_qc_results_format_valid'), table_name='audio_qc_results')
    op.drop_index(op.f('ix_audio_qc_results_clipping_detected'), table_name='audio_qc_results')
    op.drop_index(op.f('ix_audio_qc_results_silence_detected'), table_name='audio_qc_results')
    op.drop_index(op.f('ix_audio_qc_results_version_id'), table_name='audio_qc_results')
    op.drop_index(op.f('ix_audio_qc_results_cut_id'), table_name='audio_qc_results')
    op.drop_table('audio_qc_results')

    # Drop audio_deliveries table
    op.drop_index(op.f('ix_audio_deliveries_status'), table_name='audio_deliveries')
    op.drop_index(op.f('ix_audio_deliveries_copy_id'), table_name='audio_deliveries')
    op.drop_index(op.f('ix_audio_deliveries_cut_id'), table_name='audio_deliveries')
    op.drop_table('audio_deliveries')

    # Drop political_records table
    op.drop_index(op.f('ix_political_records_archived'), table_name='political_records')
    op.drop_index(op.f('ix_political_records_compliance_status'), table_name='political_records')
    op.drop_index(op.f('ix_political_records_sponsor_name'), table_name='political_records')
    op.drop_index(op.f('ix_political_records_advertiser_id'), table_name='political_records')
    op.drop_index(op.f('ix_political_records_order_id'), table_name='political_records')
    op.drop_index(op.f('ix_political_records_copy_id'), table_name='political_records')
    op.drop_table('political_records')

    # Drop live_reads table
    op.drop_index(op.f('ix_live_reads_makegood_required'), table_name='live_reads')
    op.drop_index(op.f('ix_live_reads_status'), table_name='live_reads')
    op.drop_index(op.f('ix_live_reads_confirmed'), table_name='live_reads')
    op.drop_index(op.f('ix_live_reads_performed_time'), table_name='live_reads')
    op.drop_index(op.f('ix_live_reads_scheduled_date'), table_name='live_reads')
    op.drop_index(op.f('ix_live_reads_scheduled_time'), table_name='live_reads')
    op.drop_index(op.f('ix_live_reads_advertiser_id'), table_name='live_reads')
    op.drop_index(op.f('ix_live_reads_order_id'), table_name='live_reads')
    op.drop_index(op.f('ix_live_reads_copy_id'), table_name='live_reads')
    op.drop_table('live_reads')

    # Drop audio_versions table
    op.drop_index(op.f('ix_audio_versions_cut_id'), table_name='audio_versions')
    op.drop_table('audio_versions')

    # Drop audio_cuts table
    op.drop_index(op.f('ix_audio_cuts_active'), table_name='audio_cuts')
    op.drop_index(op.f('ix_audio_cuts_expires_at'), table_name='audio_cuts')
    op.drop_index(op.f('ix_audio_cuts_cut_id'), table_name='audio_cuts')
    op.drop_index(op.f('ix_audio_cuts_copy_id'), table_name='audio_cuts')
    op.drop_table('audio_cuts')

