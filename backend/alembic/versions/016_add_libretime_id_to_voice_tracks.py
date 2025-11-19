"""Add libretime_id to voice_tracks

Revision ID: 016
Revises: 015
Create Date: 2025-01-20 12:00:00.000000

"""
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = '016'
down_revision = '015'
branch_labels = None
depends_on = None


def upgrade():
    # Add libretime_id column to voice_tracks table
    op.add_column('voice_tracks', sa.Column('libretime_id', sa.String(length=50), nullable=True))
    op.create_index(op.f('ix_voice_tracks_libretime_id'), 'voice_tracks', ['libretime_id'], unique=False)
    
    # Also update track types constraint to include VOT
    # Note: This requires dropping and recreating the constraint
    op.execute("""
        ALTER TABLE tracks 
        DROP CONSTRAINT IF EXISTS tracks_type_check;
    """)
    op.execute("""
        ALTER TABLE tracks 
        ADD CONSTRAINT tracks_type_check 
        CHECK (type IN ('MUS', 'ADV', 'PSA', 'LIN', 'INT', 'PRO', 'SHO', 'IDS', 'COM', 'NEW', 'VOT'));
    """)


def downgrade():
    # Remove index
    op.drop_index(op.f('ix_voice_tracks_libretime_id'), table_name='voice_tracks')
    
    # Remove column
    op.drop_column('voice_tracks', 'libretime_id')
    
    # Revert track types constraint (remove VOT)
    op.execute("""
        ALTER TABLE tracks 
        DROP CONSTRAINT IF EXISTS tracks_type_check;
    """)
    op.execute("""
        ALTER TABLE tracks 
        ADD CONSTRAINT tracks_type_check 
        CHECK (type IN ('MUS', 'ADV', 'PSA', 'LIN', 'INT', 'PRO', 'SHO', 'IDS', 'COM', 'NEW'));
    """)

