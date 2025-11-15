"""Add NEWS track type to tracks table

Revision ID: 013
Revises: 012
Create Date: 2024-01-22 00:00:00.000000

"""
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = '013'
down_revision = '012'
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Drop the old check constraint - PostgreSQL may name it differently
    # Use raw SQL to handle constraint name variations
    op.execute("""
        DO $$ 
        BEGIN
            IF EXISTS (
                SELECT 1 FROM pg_constraint 
                WHERE conname LIKE 'tracks_type%' 
                AND conrelid = 'tracks'::regclass
            ) THEN
                ALTER TABLE tracks DROP CONSTRAINT IF EXISTS tracks_type_check;
            END IF;
        END $$;
    """)
    
    # Add new check constraint with NEWS type
    op.create_check_constraint(
        'tracks_type_check',
        'tracks',
        sa.text("type IN ('MUS', 'ADV', 'PSA', 'LIN', 'INT', 'PRO', 'BED', 'NEW')")
    )


def downgrade() -> None:
    # Drop the new check constraint
    op.execute("""
        DO $$ 
        BEGIN
            IF EXISTS (
                SELECT 1 FROM pg_constraint 
                WHERE conname LIKE 'tracks_type%' 
                AND conrelid = 'tracks'::regclass
            ) THEN
                ALTER TABLE tracks DROP CONSTRAINT IF EXISTS tracks_type_check;
            END IF;
        END $$;
    """)
    
    # Restore old check constraint without NEWS
    op.create_check_constraint(
        'tracks_type_check',
        'tracks',
        sa.text("type IN ('MUS', 'ADV', 'PSA', 'LIN', 'INT', 'PRO', 'BED')")
    )

