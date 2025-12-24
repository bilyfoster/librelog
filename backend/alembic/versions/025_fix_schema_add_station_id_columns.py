"""Fix schema: Add station_id columns to spots and order_lines

Revision ID: 025
Revises: 024
Create Date: 2025-01-23

"""
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = '025'
down_revision = '024_split_contact_name'
branch_labels = None
depends_on = None


def upgrade():
    # Add station_id column to spots table
    # First check if column already exists (for safety)
    op.execute("""
        DO $$ 
        BEGIN
            IF NOT EXISTS (
                SELECT 1 FROM information_schema.columns 
                WHERE table_name='spots' AND column_name='station_id'
            ) THEN
                -- Add column as nullable first (for existing data)
                ALTER TABLE spots ADD COLUMN station_id INTEGER;
                
                -- Set a default station_id for existing spots (use first station if available)
                UPDATE spots SET station_id = (
                    SELECT id FROM stations LIMIT 1
                ) WHERE station_id IS NULL;
                
                -- Now make it NOT NULL and add constraints
                ALTER TABLE spots 
                ALTER COLUMN station_id SET NOT NULL,
                ADD CONSTRAINT fk_spots_station_id 
                    FOREIGN KEY (station_id) REFERENCES stations(id);
                
                CREATE INDEX IF NOT EXISTS ix_spots_station_id ON spots(station_id);
            END IF;
        END $$;
    """)
    
    # Add station_id column to order_lines table
    # Keep the existing 'station' column for backward compatibility
    op.execute("""
        DO $$ 
        BEGIN
            IF NOT EXISTS (
                SELECT 1 FROM information_schema.columns 
                WHERE table_name='order_lines' AND column_name='station_id'
            ) THEN
                -- Add column as nullable first (for existing data)
                ALTER TABLE order_lines ADD COLUMN station_id INTEGER;
                
                -- Set a default station_id for existing order_lines (use first station if available)
                UPDATE order_lines SET station_id = (
                    SELECT id FROM stations LIMIT 1
                ) WHERE station_id IS NULL;
                
                -- Now make it NOT NULL and add constraints
                ALTER TABLE order_lines 
                ALTER COLUMN station_id SET NOT NULL,
                ADD CONSTRAINT fk_order_lines_station_id 
                    FOREIGN KEY (station_id) REFERENCES stations(id);
                
                CREATE INDEX IF NOT EXISTS ix_order_lines_station_id ON order_lines(station_id);
            END IF;
        END $$;
    """)


def downgrade():
    # Remove station_id column from order_lines
    op.execute("""
        DO $$ 
        BEGIN
            IF EXISTS (
                SELECT 1 FROM information_schema.columns 
                WHERE table_name='order_lines' AND column_name='station_id'
            ) THEN
                DROP INDEX IF EXISTS ix_order_lines_station_id;
                ALTER TABLE order_lines DROP CONSTRAINT IF EXISTS fk_order_lines_station_id;
                ALTER TABLE order_lines DROP COLUMN IF EXISTS station_id;
            END IF;
        END $$;
    """)
    
    # Remove station_id column from spots
    op.execute("""
        DO $$ 
        BEGIN
            IF EXISTS (
                SELECT 1 FROM information_schema.columns 
                WHERE table_name='spots' AND column_name='station_id'
            ) THEN
                DROP INDEX IF EXISTS ix_spots_station_id;
                ALTER TABLE spots DROP CONSTRAINT IF EXISTS fk_spots_station_id;
                ALTER TABLE spots DROP COLUMN IF EXISTS station_id;
            END IF;
        END $$;
    """)

