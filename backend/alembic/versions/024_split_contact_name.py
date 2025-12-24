"""split contact_name into first_name and last_name

Revision ID: 024_split_contact_name
Revises: 023_add_station_support
Create Date: 2025-01-22 12:00:00.000000

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy import text


# revision identifiers, used by Alembic.
revision = '024_split_contact_name'
down_revision = '018'
branch_labels = None
depends_on = None


def upgrade():
    # Add new columns to advertisers table
    op.add_column('advertisers', sa.Column('contact_first_name', sa.String(length=100), nullable=True))
    op.add_column('advertisers', sa.Column('contact_last_name', sa.String(length=100), nullable=True))
    
    # Migrate existing contact_name data
    # Split contact_name into first_name and last_name
    connection = op.get_bind()
    result = connection.execute(text("SELECT id, contact_name FROM advertisers WHERE contact_name IS NOT NULL"))
    for row in result:
        advertiser_id, contact_name = row
        if contact_name:
            # Split by space - take first word as first_name, rest as last_name
            parts = contact_name.strip().split(None, 1)
            first_name = parts[0] if parts else None
            last_name = parts[1] if len(parts) > 1 else None
            
            connection.execute(
                text("UPDATE advertisers SET contact_first_name = :first_name, contact_last_name = :last_name WHERE id = :id"),
                {"first_name": first_name, "last_name": last_name, "id": advertiser_id}
            )
    
    # Drop the old contact_name column
    op.drop_column('advertisers', 'contact_name')
    
    # Add new columns to agencies table
    op.add_column('agencies', sa.Column('contact_first_name', sa.String(length=100), nullable=True))
    op.add_column('agencies', sa.Column('contact_last_name', sa.String(length=100), nullable=True))
    
    # Migrate existing contact_name data for agencies
    result = connection.execute(text("SELECT id, contact_name FROM agencies WHERE contact_name IS NOT NULL"))
    for row in result:
        agency_id, contact_name = row
        if contact_name:
            # Split by space - take first word as first_name, rest as last_name
            parts = contact_name.strip().split(None, 1)
            first_name = parts[0] if parts else None
            last_name = parts[1] if len(parts) > 1 else None
            
            connection.execute(
                text("UPDATE agencies SET contact_first_name = :first_name, contact_last_name = :last_name WHERE id = :id"),
                {"first_name": first_name, "last_name": last_name, "id": agency_id}
            )
    
    # Drop the old contact_name column
    op.drop_column('agencies', 'contact_name')


def downgrade():
    # Re-add contact_name columns
    op.add_column('advertisers', sa.Column('contact_name', sa.String(length=255), nullable=True))
    op.add_column('agencies', sa.Column('contact_name', sa.String(length=255), nullable=True))
    
    # Combine first_name and last_name back into contact_name
    connection = op.get_bind()
    
    # For advertisers
    result = connection.execute(text("SELECT id, contact_first_name, contact_last_name FROM advertisers"))
    for row in result:
        advertiser_id, first_name, last_name = row
        if first_name or last_name:
            contact_name = ' '.join(filter(None, [first_name, last_name]))
            connection.execute(
                text("UPDATE advertisers SET contact_name = :contact_name WHERE id = :id"),
                {"contact_name": contact_name, "id": advertiser_id}
            )
    
    # For agencies
    result = connection.execute(text("SELECT id, contact_first_name, contact_last_name FROM agencies"))
    for row in result:
        agency_id, first_name, last_name = row
        if first_name or last_name:
            contact_name = ' '.join(filter(None, [first_name, last_name]))
            connection.execute(
                text("UPDATE agencies SET contact_name = :contact_name WHERE id = :id"),
                {"contact_name": contact_name, "id": agency_id}
            )
    
    # Drop the new columns
    op.drop_column('advertisers', 'contact_last_name')
    op.drop_column('advertisers', 'contact_first_name')
    op.drop_column('agencies', 'contact_last_name')
    op.drop_column('agencies', 'contact_first_name')


