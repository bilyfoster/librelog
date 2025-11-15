"""Add billing tables

Revision ID: 005
Revises: 004
Create Date: 2024-01-15 00:00:00.000000

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = '005'
down_revision = '004'
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Create enum type
    op.execute("CREATE TYPE invoicestatus AS ENUM ('DRAFT', 'SENT', 'PAID', 'OVERDUE', 'CANCELLED')")
    
    # Create invoices table
    op.create_table('invoices',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('invoice_number', sa.String(length=50), nullable=False),
        sa.Column('advertiser_id', sa.Integer(), nullable=False),
        sa.Column('agency_id', sa.Integer(), nullable=True),
        sa.Column('order_id', sa.Integer(), nullable=True),
        sa.Column('campaign_id', sa.Integer(), nullable=True),
        sa.Column('invoice_date', sa.Date(), nullable=False),
        sa.Column('due_date', sa.Date(), nullable=False),
        sa.Column('subtotal', sa.Numeric(10, 2), nullable=True, server_default='0'),
        sa.Column('tax', sa.Numeric(10, 2), nullable=True, server_default='0'),
        sa.Column('total', sa.Numeric(10, 2), nullable=True, server_default='0'),
        sa.Column('status', postgresql.ENUM('DRAFT', 'SENT', 'PAID', 'OVERDUE', 'CANCELLED', name='invoicestatus'), nullable=False, server_default='DRAFT'),
        sa.Column('payment_terms', sa.String(length=100), nullable=True),
        sa.Column('notes', sa.String(length=500), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['advertiser_id'], ['advertisers.id'], ),
        sa.ForeignKeyConstraint(['agency_id'], ['agencies.id'], ),
        sa.ForeignKeyConstraint(['order_id'], ['orders.id'], ),
        sa.ForeignKeyConstraint(['campaign_id'], ['campaigns.id'], ),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('invoice_number')
    )
    op.create_index(op.f('ix_invoices_id'), 'invoices', ['id'], unique=False)
    op.create_index(op.f('ix_invoices_invoice_number'), 'invoices', ['invoice_number'], unique=True)
    op.create_index(op.f('ix_invoices_advertiser_id'), 'invoices', ['advertiser_id'], unique=False)
    op.create_index(op.f('ix_invoices_agency_id'), 'invoices', ['agency_id'], unique=False)
    op.create_index(op.f('ix_invoices_order_id'), 'invoices', ['order_id'], unique=False)
    op.create_index(op.f('ix_invoices_campaign_id'), 'invoices', ['campaign_id'], unique=False)
    op.create_index(op.f('ix_invoices_invoice_date'), 'invoices', ['invoice_date'], unique=False)
    op.create_index(op.f('ix_invoices_due_date'), 'invoices', ['due_date'], unique=False)
    op.create_index(op.f('ix_invoices_status'), 'invoices', ['status'], unique=False)
    
    # Create invoice_lines table
    op.create_table('invoice_lines',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('invoice_id', sa.Integer(), nullable=False),
        sa.Column('description', sa.String(length=255), nullable=False),
        sa.Column('quantity', sa.Integer(), nullable=True, server_default='1'),
        sa.Column('unit_price', sa.Numeric(10, 2), nullable=False),
        sa.Column('total', sa.Numeric(10, 2), nullable=False),
        sa.Column('spot_ids', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['invoice_id'], ['invoices.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_invoice_lines_id'), 'invoice_lines', ['id'], unique=False)
    op.create_index(op.f('ix_invoice_lines_invoice_id'), 'invoice_lines', ['invoice_id'], unique=False)
    
    # Create payments table
    op.create_table('payments',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('invoice_id', sa.Integer(), nullable=False),
        sa.Column('amount', sa.Numeric(10, 2), nullable=False),
        sa.Column('payment_date', sa.DateTime(timezone=True), nullable=False),
        sa.Column('payment_method', sa.String(length=50), nullable=True),
        sa.Column('reference_number', sa.String(length=100), nullable=True),
        sa.Column('notes', sa.String(length=500), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['invoice_id'], ['invoices.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_payments_id'), 'payments', ['id'], unique=False)
    op.create_index(op.f('ix_payments_invoice_id'), 'payments', ['invoice_id'], unique=False)
    op.create_index(op.f('ix_payments_payment_date'), 'payments', ['payment_date'], unique=False)
    
    # Create makegoods table
    op.create_table('makegoods',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('original_spot_id', sa.Integer(), nullable=False),
        sa.Column('makegood_spot_id', sa.Integer(), nullable=False),
        sa.Column('campaign_id', sa.Integer(), nullable=True),
        sa.Column('reason', sa.String(length=500), nullable=True),
        sa.Column('approved_by', sa.Integer(), nullable=True),
        sa.Column('approved_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['approved_by'], ['users.id'], ),
        sa.ForeignKeyConstraint(['campaign_id'], ['campaigns.id'], ),
        sa.ForeignKeyConstraint(['makegood_spot_id'], ['spots.id'], ),
        sa.ForeignKeyConstraint(['original_spot_id'], ['spots.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_makegoods_id'), 'makegoods', ['id'], unique=False)
    op.create_index(op.f('ix_makegoods_original_spot_id'), 'makegoods', ['original_spot_id'], unique=False)
    op.create_index(op.f('ix_makegoods_makegood_spot_id'), 'makegoods', ['makegood_spot_id'], unique=False)
    op.create_index(op.f('ix_makegoods_campaign_id'), 'makegoods', ['campaign_id'], unique=False)


def downgrade() -> None:
    # Drop makegoods table
    op.drop_index(op.f('ix_makegoods_campaign_id'), table_name='makegoods')
    op.drop_index(op.f('ix_makegoods_makegood_spot_id'), table_name='makegoods')
    op.drop_index(op.f('ix_makegoods_original_spot_id'), table_name='makegoods')
    op.drop_index(op.f('ix_makegoods_id'), table_name='makegoods')
    op.drop_table('makegoods')
    
    # Drop payments table
    op.drop_index(op.f('ix_payments_payment_date'), table_name='payments')
    op.drop_index(op.f('ix_payments_invoice_id'), table_name='payments')
    op.drop_index(op.f('ix_payments_id'), table_name='payments')
    op.drop_table('payments')
    
    # Drop invoice_lines table
    op.drop_index(op.f('ix_invoice_lines_invoice_id'), table_name='invoice_lines')
    op.drop_index(op.f('ix_invoice_lines_id'), table_name='invoice_lines')
    op.drop_table('invoice_lines')
    
    # Drop invoices table
    op.drop_index(op.f('ix_invoices_status'), table_name='invoices')
    op.drop_index(op.f('ix_invoices_due_date'), table_name='invoices')
    op.drop_index(op.f('ix_invoices_invoice_date'), table_name='invoices')
    op.drop_index(op.f('ix_invoices_campaign_id'), table_name='invoices')
    op.drop_index(op.f('ix_invoices_order_id'), table_name='invoices')
    op.drop_index(op.f('ix_invoices_agency_id'), table_name='invoices')
    op.drop_index(op.f('ix_invoices_advertiser_id'), table_name='invoices')
    op.drop_index(op.f('ix_invoices_invoice_number'), table_name='invoices')
    op.drop_index(op.f('ix_invoices_id'), table_name='invoices')
    op.drop_table('invoices')
    
    # Drop enum type
    op.execute("DROP TYPE IF EXISTS invoicestatus")

