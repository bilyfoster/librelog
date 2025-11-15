"""Add copy management tables

Revision ID: 004
Revises: 003
Create Date: 2024-01-15 00:00:00.000000

"""
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = '004'
down_revision = '003'
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Create copy table
    op.create_table('copy',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('order_id', sa.Integer(), nullable=True),
        sa.Column('advertiser_id', sa.Integer(), nullable=True),
        sa.Column('title', sa.String(length=255), nullable=False),
        sa.Column('script_text', sa.Text(), nullable=True),
        sa.Column('audio_file_path', sa.Text(), nullable=True),
        sa.Column('audio_file_url', sa.Text(), nullable=True),
        sa.Column('version', sa.Integer(), nullable=True, server_default='1'),
        sa.Column('expires_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=True, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['advertiser_id'], ['advertisers.id'], ),
        sa.ForeignKeyConstraint(['order_id'], ['orders.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_copy_id'), 'copy', ['id'], unique=False)
    op.create_index(op.f('ix_copy_order_id'), 'copy', ['order_id'], unique=False)
    op.create_index(op.f('ix_copy_advertiser_id'), 'copy', ['advertiser_id'], unique=False)
    op.create_index(op.f('ix_copy_title'), 'copy', ['title'], unique=False)
    op.create_index(op.f('ix_copy_expires_at'), 'copy', ['expires_at'], unique=False)
    op.create_index(op.f('ix_copy_active'), 'copy', ['active'], unique=False)
    
    # Create copy_assignments table
    op.create_table('copy_assignments',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('spot_id', sa.Integer(), nullable=False),
        sa.Column('copy_id', sa.Integer(), nullable=False),
        sa.Column('order_id', sa.Integer(), nullable=True),
        sa.Column('assigned_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('assigned_by', sa.Integer(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['assigned_by'], ['users.id'], ),
        sa.ForeignKeyConstraint(['copy_id'], ['copy.id'], ),
        sa.ForeignKeyConstraint(['order_id'], ['orders.id'], ),
        sa.ForeignKeyConstraint(['spot_id'], ['spots.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_copy_assignments_id'), 'copy_assignments', ['id'], unique=False)
    op.create_index(op.f('ix_copy_assignments_spot_id'), 'copy_assignments', ['spot_id'], unique=False)
    op.create_index(op.f('ix_copy_assignments_copy_id'), 'copy_assignments', ['copy_id'], unique=False)
    op.create_index(op.f('ix_copy_assignments_order_id'), 'copy_assignments', ['order_id'], unique=False)


def downgrade() -> None:
    # Drop copy_assignments table
    op.drop_index(op.f('ix_copy_assignments_order_id'), table_name='copy_assignments')
    op.drop_index(op.f('ix_copy_assignments_copy_id'), table_name='copy_assignments')
    op.drop_index(op.f('ix_copy_assignments_spot_id'), table_name='copy_assignments')
    op.drop_index(op.f('ix_copy_assignments_id'), table_name='copy_assignments')
    op.drop_table('copy_assignments')
    
    # Drop copy table
    op.drop_index(op.f('ix_copy_active'), table_name='copy')
    op.drop_index(op.f('ix_copy_expires_at'), table_name='copy')
    op.drop_index(op.f('ix_copy_title'), table_name='copy')
    op.drop_index(op.f('ix_copy_advertiser_id'), table_name='copy')
    op.drop_index(op.f('ix_copy_order_id'), table_name='copy')
    op.drop_index(op.f('ix_copy_id'), table_name='copy')
    op.drop_table('copy')

