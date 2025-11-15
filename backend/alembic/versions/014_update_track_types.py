"""Update track types constraint

Revision ID: 014
Revises: 013
Create Date: 2025-11-15 01:00:00.000000

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '014'
down_revision = '013'
branch_labels = None
depends_on = None


def upgrade():
    # Drop the old constraint
    op.execute("""
        DO $$
        BEGIN
            IF EXISTS (
                SELECT 1 FROM pg_constraint 
                WHERE conrelid = 'tracks'::regclass 
                AND conname = 'tracks_type_check'
            ) THEN
                ALTER TABLE tracks DROP CONSTRAINT tracks_type_check;
            END IF;
        END $$;
    """)
    
    # Add the new constraint with all track types
    op.execute("""
        ALTER TABLE tracks 
        ADD CONSTRAINT tracks_type_check 
        CHECK (type IN ('MUS', 'ADV', 'PSA', 'LIN', 'INT', 'PRO', 'SHO', 'IDS', 'COM', 'NEW'));
    """)


def downgrade():
    # Revert to previous constraint (without SHO, IDS, COM, but with BED)
    op.execute("""
        DO $$
        BEGIN
            IF EXISTS (
                SELECT 1 FROM pg_constraint 
                WHERE conrelid = 'tracks'::regclass 
                AND conname = 'tracks_type_check'
            ) THEN
                ALTER TABLE tracks DROP CONSTRAINT tracks_type_check;
            END IF;
        END $$;
    """)
    
    op.execute("""
        ALTER TABLE tracks 
        ADD CONSTRAINT tracks_type_check 
        CHECK (type IN ('MUS', 'ADV', 'PSA', 'LIN', 'INT', 'PRO', 'BED', 'NEW'));
    """)

