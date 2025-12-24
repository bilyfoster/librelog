"""WideOrbit-compatible sales order schema

Revision ID: 021
Revises: 020
Create Date: 2025-01-XX

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = '021'
down_revision = '020'
branch_labels = None
depends_on = None


def upgrade():
    # Create enum types (only if they don't exist)
    enum_types = [
        ("ordertype", "('LOCAL', 'NATIONAL', 'NETWORK', 'DIGITAL', 'NTR')"),
        ("billingcycle", "('WEEKLY', 'MONTHLY', 'ONE_SHOT', 'BIWEEKLY', 'QUARTERLY')"),
        ("invoicetype", "('STANDARD', 'COOP', 'POLITICAL')"),
        ("politicalclass", "('FEDERAL', 'STATE', 'LOCAL', 'ISSUE')"),
        ("revenuetype", "('SPOT', 'DIGITAL', 'TRADE', 'PROMOTION', 'NTR')"),
        ("selloutclass", "('ROS', 'FIXED_POSITION', 'BONUS', 'GUARANTEED')"),
        ("attachmenttype", "('CONTRACT', 'AUDIO', 'SCRIPT', 'CREATIVE', 'LEGAL', 'IO', 'EMAIL', 'OTHER')"),
        ("revisionreasoncode", "('PRICE_CHANGE', 'EXTENSION', 'CANCELLATION', 'LINE_ADDED', 'LINE_REMOVED', 'COPY_CHANGE', 'DATE_CHANGE', 'OTHER')"),
        ("workflowstate", "('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'TRAFFIC_READY', 'BILLING_READY', 'LOCKED', 'CANCELLED')"),
        ("copytype", "('NEW', 'TAG_UPDATE', 'RENEWAL', 'SEASONAL')"),
        ("revenuebucket", "('SPOT', 'DIGITAL', 'TRADE', 'PROMOTIONS')"),
    ]
    
    for enum_name, enum_values in enum_types:
        op.execute(f"""
            DO $$ BEGIN
                CREATE TYPE {enum_name} AS ENUM {enum_values};
            EXCEPTION
                WHEN duplicate_object THEN null;
            END $$;
        """)
    
    # Enhance orders table
    op.add_column('orders', sa.Column('order_name', sa.String(length=255), nullable=True))
    op.create_index(op.f('ix_orders_order_name'), 'orders', ['order_name'], unique=False)
    op.add_column('orders', sa.Column('sales_team', sa.String(length=100), nullable=True))
    op.add_column('orders', sa.Column('sales_office', sa.String(length=100), nullable=True))
    op.add_column('orders', sa.Column('sales_region', sa.String(length=100), nullable=True))
    op.add_column('orders', sa.Column('stations', postgresql.JSONB(astext_type=sa.Text()), nullable=True))
    op.add_column('orders', sa.Column('cluster', sa.String(length=100), nullable=True))
    op.add_column('orders', sa.Column('order_type', postgresql.ENUM('LOCAL', 'NATIONAL', 'NETWORK', 'DIGITAL', 'NTR', name='ordertype', create_type=False), nullable=True))
    op.create_index(op.f('ix_orders_order_type'), 'orders', ['order_type'], unique=False)
    
    # Financial terms
    op.add_column('orders', sa.Column('gross_amount', sa.Numeric(12, 2), nullable=True, server_default='0'))
    op.add_column('orders', sa.Column('net_amount', sa.Numeric(12, 2), nullable=True, server_default='0'))
    op.add_column('orders', sa.Column('agency_commission_percent', sa.Numeric(5, 2), nullable=True))
    op.add_column('orders', sa.Column('agency_commission_amount', sa.Numeric(12, 2), nullable=True))
    op.add_column('orders', sa.Column('agency_discount', sa.Numeric(12, 2), nullable=True))
    op.add_column('orders', sa.Column('cash_discount', sa.Numeric(12, 2), nullable=True))
    op.add_column('orders', sa.Column('trade_barter', sa.Boolean(), nullable=True, server_default='false'))
    op.create_index(op.f('ix_orders_trade_barter'), 'orders', ['trade_barter'], unique=False)
    op.add_column('orders', sa.Column('trade_value', sa.Numeric(12, 2), nullable=True))
    op.add_column('orders', sa.Column('taxable', sa.Boolean(), nullable=True, server_default='false'))
    op.create_index(op.f('ix_orders_taxable'), 'orders', ['taxable'], unique=False)
    op.add_column('orders', sa.Column('billing_cycle', postgresql.ENUM('WEEKLY', 'MONTHLY', 'ONE_SHOT', 'BIWEEKLY', 'QUARTERLY', name='billingcycle', create_type=False), nullable=True))
    op.add_column('orders', sa.Column('invoice_type', postgresql.ENUM('STANDARD', 'COOP', 'POLITICAL', name='invoicetype', create_type=False), nullable=True))
    
    # Co-op fields
    op.add_column('orders', sa.Column('coop_sponsor', sa.String(length=255), nullable=True))
    op.add_column('orders', sa.Column('coop_percent', sa.Numeric(5, 2), nullable=True))
    
    # Client PO
    op.add_column('orders', sa.Column('client_po_number', sa.String(length=100), nullable=True))
    op.create_index(op.f('ix_orders_client_po_number'), 'orders', ['client_po_number'], unique=False)
    
    # Billing address/contact
    op.add_column('orders', sa.Column('billing_address', sa.Text(), nullable=True))
    op.add_column('orders', sa.Column('billing_contact', sa.String(length=255), nullable=True))
    op.add_column('orders', sa.Column('billing_contact_email', sa.String(length=255), nullable=True))
    op.add_column('orders', sa.Column('billing_contact_phone', sa.String(length=50), nullable=True))
    
    # Legal/Compliance
    op.add_column('orders', sa.Column('political_class', postgresql.ENUM('FEDERAL', 'STATE', 'LOCAL', 'ISSUE', name='politicalclass', create_type=False), nullable=True))
    op.create_index(op.f('ix_orders_political_class'), 'orders', ['political_class'], unique=False)
    op.add_column('orders', sa.Column('political_window_flag', sa.Boolean(), nullable=True, server_default='false'))
    op.create_index(op.f('ix_orders_political_window_flag'), 'orders', ['political_window_flag'], unique=False)
    op.add_column('orders', sa.Column('contract_reference', sa.String(length=100), nullable=True))
    op.create_index(op.f('ix_orders_contract_reference'), 'orders', ['contract_reference'], unique=False)
    op.add_column('orders', sa.Column('insertion_order_number', sa.String(length=100), nullable=True))
    op.create_index(op.f('ix_orders_insertion_order_number'), 'orders', ['insertion_order_number'], unique=False)
    op.add_column('orders', sa.Column('regulatory_notes', sa.Text(), nullable=True))
    op.add_column('orders', sa.Column('fcc_id', sa.String(length=100), nullable=True))
    op.add_column('orders', sa.Column('required_disclaimers', sa.Text(), nullable=True))
    
    # Workflow flags
    op.add_column('orders', sa.Column('traffic_ready', sa.Boolean(), nullable=True, server_default='false'))
    op.create_index(op.f('ix_orders_traffic_ready'), 'orders', ['traffic_ready'], unique=False)
    op.add_column('orders', sa.Column('billing_ready', sa.Boolean(), nullable=True, server_default='false'))
    op.create_index(op.f('ix_orders_billing_ready'), 'orders', ['billing_ready'], unique=False)
    op.add_column('orders', sa.Column('locked', sa.Boolean(), nullable=True, server_default='false'))
    op.create_index(op.f('ix_orders_locked'), 'orders', ['locked'], unique=False)
    op.add_column('orders', sa.Column('revision_number', sa.Integer(), nullable=True, server_default='1'))
    op.add_column('orders', sa.Column('created_by', sa.Integer(), nullable=True))
    op.create_foreign_key('fk_orders_created_by', 'orders', 'users', ['created_by'], ['id'])
    
    # Create order_lines table
    op.create_table('order_lines',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('order_id', sa.Integer(), nullable=False),
        sa.Column('line_number', sa.Integer(), nullable=False),
        sa.Column('start_date', sa.Date(), nullable=False),
        sa.Column('end_date', sa.Date(), nullable=False),
        sa.Column('station', sa.String(length=100), nullable=True),
        sa.Column('product', sa.String(length=255), nullable=True),
        sa.Column('revenue_type', postgresql.ENUM('SPOT', 'DIGITAL', 'TRADE', 'PROMOTION', 'NTR', name='revenuetype', create_type=False), nullable=True),
        sa.Column('length', sa.Integer(), nullable=True),
        sa.Column('daypart', sa.String(length=50), nullable=True),
        sa.Column('days_of_week', sa.String(length=7), nullable=True),
        sa.Column('rate', sa.Numeric(12, 2), nullable=True),
        sa.Column('rate_type', sa.String(length=50), nullable=True),
        sa.Column('sellout_class', postgresql.ENUM('ROS', 'FIXED_POSITION', 'BONUS', 'GUARANTEED', name='selloutclass', create_type=False), nullable=True),
        sa.Column('priority_code', sa.String(length=50), nullable=True),
        sa.Column('spot_frequency', sa.Integer(), nullable=True),
        sa.Column('estimated_impressions', sa.Integer(), nullable=True),
        sa.Column('cpm', sa.Numeric(10, 2), nullable=True),
        sa.Column('cpp', sa.Numeric(10, 2), nullable=True),
        sa.Column('makegood_eligible', sa.Boolean(), nullable=True, server_default='true'),
        sa.Column('guaranteed_position', sa.Boolean(), nullable=True, server_default='false'),
        sa.Column('preemptible', sa.Boolean(), nullable=True, server_default='true'),
        sa.Column('inventory_class', sa.String(length=100), nullable=True),
        sa.Column('product_code', sa.String(length=100), nullable=True),
        sa.Column('deal_points', sa.Text(), nullable=True),
        sa.Column('notes', sa.Text(), nullable=True),
        sa.Column('platform', sa.String(length=100), nullable=True),
        sa.Column('impressions_booked', sa.Integer(), nullable=True),
        sa.Column('delivery_window', sa.String(length=100), nullable=True),
        sa.Column('targeting_parameters', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('companion_banners', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.PrimaryKeyConstraint('id'),
        sa.ForeignKeyConstraint(['order_id'], ['orders.id'], )
    )
    op.create_index(op.f('ix_order_lines_order_id'), 'order_lines', ['order_id'], unique=False)
    op.create_index(op.f('ix_order_lines_start_date'), 'order_lines', ['start_date'], unique=False)
    op.create_index(op.f('ix_order_lines_end_date'), 'order_lines', ['end_date'], unique=False)
    op.create_index(op.f('ix_order_lines_station'), 'order_lines', ['station'], unique=False)
    op.create_index(op.f('ix_order_lines_revenue_type'), 'order_lines', ['revenue_type'], unique=False)
    op.create_index(op.f('ix_order_lines_daypart'), 'order_lines', ['daypart'], unique=False)
    op.create_index(op.f('ix_order_lines_priority_code'), 'order_lines', ['priority_code'], unique=False)
    op.create_index(op.f('ix_order_lines_makegood_eligible'), 'order_lines', ['makegood_eligible'], unique=False)
    op.create_index(op.f('ix_order_lines_guaranteed_position'), 'order_lines', ['guaranteed_position'], unique=False)
    op.create_index(op.f('ix_order_lines_preemptible'), 'order_lines', ['preemptible'], unique=False)
    op.create_index(op.f('ix_order_lines_product_code'), 'order_lines', ['product_code'], unique=False)
    
    # Create order_attachments table
    op.create_table('order_attachments',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('order_id', sa.Integer(), nullable=True),
        sa.Column('order_line_id', sa.Integer(), nullable=True),
        sa.Column('copy_id', sa.Integer(), nullable=True),
        sa.Column('file_path', sa.Text(), nullable=False),
        sa.Column('file_name', sa.String(length=255), nullable=False),
        sa.Column('mime_type', sa.String(length=100), nullable=True),
        sa.Column('file_size', sa.BigInteger(), nullable=True),
        sa.Column('attachment_type', postgresql.ENUM('CONTRACT', 'AUDIO', 'SCRIPT', 'CREATIVE', 'LEGAL', 'IO', 'EMAIL', 'OTHER', name='attachmenttype', create_type=False), nullable=False),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('notes', sa.Text(), nullable=True),
        sa.Column('uploaded_by', sa.Integer(), nullable=False),
        sa.Column('uploaded_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.PrimaryKeyConstraint('id'),
        sa.ForeignKeyConstraint(['order_id'], ['orders.id'], ),
        sa.ForeignKeyConstraint(['order_line_id'], ['order_lines.id'], ),
        sa.ForeignKeyConstraint(['copy_id'], ['copy.id'], ),
        sa.ForeignKeyConstraint(['uploaded_by'], ['users.id'], )
    )
    op.create_index(op.f('ix_order_attachments_order_id'), 'order_attachments', ['order_id'], unique=False)
    op.create_index(op.f('ix_order_attachments_order_line_id'), 'order_attachments', ['order_line_id'], unique=False)
    op.create_index(op.f('ix_order_attachments_copy_id'), 'order_attachments', ['copy_id'], unique=False)
    op.create_index(op.f('ix_order_attachments_attachment_type'), 'order_attachments', ['attachment_type'], unique=False)
    op.create_index(op.f('ix_order_attachments_uploaded_at'), 'order_attachments', ['uploaded_at'], unique=False)
    
    # Create order_revisions table
    op.create_table('order_revisions',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('order_id', sa.Integer(), nullable=False),
        sa.Column('revision_number', sa.Integer(), nullable=False),
        sa.Column('changed_fields', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('reason_code', postgresql.ENUM('PRICE_CHANGE', 'EXTENSION', 'CANCELLATION', 'LINE_ADDED', 'LINE_REMOVED', 'COPY_CHANGE', 'DATE_CHANGE', 'OTHER', name='revisionreasoncode', create_type=False), nullable=True),
        sa.Column('reason_notes', sa.Text(), nullable=True),
        sa.Column('approval_status_at_revision', sa.String(length=50), nullable=True),
        sa.Column('changed_by', sa.Integer(), nullable=False),
        sa.Column('changed_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.PrimaryKeyConstraint('id'),
        sa.ForeignKeyConstraint(['order_id'], ['orders.id'], ),
        sa.ForeignKeyConstraint(['changed_by'], ['users.id'], )
    )
    op.create_index(op.f('ix_order_revisions_order_id'), 'order_revisions', ['order_id'], unique=False)
    op.create_index(op.f('ix_order_revisions_revision_number'), 'order_revisions', ['revision_number'], unique=False)
    op.create_index(op.f('ix_order_revisions_reason_code'), 'order_revisions', ['reason_code'], unique=False)
    op.create_index(op.f('ix_order_revisions_changed_at'), 'order_revisions', ['changed_at'], unique=False)
    
    # Create order_workflow_states table
    op.create_table('order_workflow_states',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('order_id', sa.Integer(), nullable=False),
        sa.Column('state', postgresql.ENUM('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'TRAFFIC_READY', 'BILLING_READY', 'LOCKED', 'CANCELLED', name='workflowstate', create_type=False), nullable=False),
        sa.Column('notes', sa.Text(), nullable=True),
        sa.Column('state_changed_by', sa.Integer(), nullable=False),
        sa.Column('state_changed_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.PrimaryKeyConstraint('id'),
        sa.ForeignKeyConstraint(['order_id'], ['orders.id'], ),
        sa.ForeignKeyConstraint(['state_changed_by'], ['users.id'], )
    )
    op.create_index(op.f('ix_order_workflow_states_order_id'), 'order_workflow_states', ['order_id'], unique=False)
    op.create_index(op.f('ix_order_workflow_states_state'), 'order_workflow_states', ['state'], unique=False)
    op.create_index(op.f('ix_order_workflow_states_state_changed_at'), 'order_workflow_states', ['state_changed_at'], unique=False)
    
    # Enhance copy table
    op.add_column('copy', sa.Column('copy_code', sa.String(length=100), nullable=True))
    op.create_index(op.f('ix_copy_copy_code'), 'copy', ['copy_code'], unique=False)
    op.add_column('copy', sa.Column('isci_code', sa.String(length=100), nullable=True))
    op.create_index(op.f('ix_copy_isci_code'), 'copy', ['isci_code'], unique=False)
    op.add_column('copy', sa.Column('copy_type', postgresql.ENUM('NEW', 'TAG_UPDATE', 'RENEWAL', 'SEASONAL', name='copytype', create_type=False), nullable=True))
    op.create_index(op.f('ix_copy_copy_type'), 'copy', ['copy_type'], unique=False)
    op.add_column('copy', sa.Column('client_provided_audio', sa.Text(), nullable=True))
    op.add_column('copy', sa.Column('effective_date', sa.Date(), nullable=True))
    op.create_index(op.f('ix_copy_effective_date'), 'copy', ['effective_date'], unique=False)
    op.add_column('copy', sa.Column('expiration_date', sa.Date(), nullable=True))
    op.create_index(op.f('ix_copy_expiration_date'), 'copy', ['expiration_date'], unique=False)
    op.add_column('copy', sa.Column('priority', sa.Integer(), nullable=True))
    op.add_column('copy', sa.Column('production_notes', sa.Text(), nullable=True))
    op.add_column('copy', sa.Column('legal_copy', sa.Text(), nullable=True))
    op.add_column('copy', sa.Column('required_disclaimers', sa.Text(), nullable=True))
    op.add_column('copy', sa.Column('talent_restrictions', sa.Text(), nullable=True))
    op.add_column('copy', sa.Column('aqh_reconciliation', postgresql.JSONB(astext_type=sa.Text()), nullable=True))
    op.add_column('copy', sa.Column('audio_format', sa.String(length=50), nullable=True))
    op.add_column('copy', sa.Column('audio_source', sa.String(length=100), nullable=True))
    op.add_column('copy', sa.Column('loudness_check', sa.Boolean(), nullable=True, server_default='false'))
    op.create_index(op.f('ix_copy_loudness_check'), 'copy', ['loudness_check'], unique=False)
    
    # Enhance invoices table
    op.add_column('invoices', sa.Column('billing_start', sa.Date(), nullable=True))
    op.create_index(op.f('ix_invoices_billing_start'), 'invoices', ['billing_start'], unique=False)
    op.add_column('invoices', sa.Column('billing_end', sa.Date(), nullable=True))
    op.create_index(op.f('ix_invoices_billing_end'), 'invoices', ['billing_end'], unique=False)
    op.add_column('invoices', sa.Column('billing_schedule', sa.Text(), nullable=True))
    op.add_column('invoices', sa.Column('billing_notes', sa.Text(), nullable=True))
    op.add_column('invoices', sa.Column('invoice_grouping_code', sa.String(length=100), nullable=True))
    op.create_index(op.f('ix_invoices_invoice_grouping_code'), 'invoices', ['invoice_grouping_code'], unique=False)
    op.add_column('invoices', sa.Column('revenue_class', sa.String(length=100), nullable=True))
    op.add_column('invoices', sa.Column('gl_account', sa.String(length=100), nullable=True))
    op.add_column('invoices', sa.Column('revenue_bucket', postgresql.ENUM('SPOT', 'DIGITAL', 'TRADE', 'PROMOTIONS', name='revenuebucket', create_type=False), nullable=True))
    op.create_index(op.f('ix_invoices_revenue_bucket'), 'invoices', ['revenue_bucket'], unique=False)
    op.add_column('invoices', sa.Column('prepaid', sa.Boolean(), nullable=True, server_default='false'))
    op.create_index(op.f('ix_invoices_prepaid'), 'invoices', ['prepaid'], unique=False)
    op.add_column('invoices', sa.Column('credits_issued', sa.Numeric(10, 2), nullable=True, server_default='0'))
    op.add_column('invoices', sa.Column('adjustments', sa.Numeric(10, 2), nullable=True, server_default='0'))
    op.add_column('invoices', sa.Column('sponsorship_fee', sa.Numeric(10, 2), nullable=True))
    op.add_column('invoices', sa.Column('election_cycle', sa.String(length=100), nullable=True))
    op.add_column('invoices', sa.Column('lowest_unit_rate_record', sa.Text(), nullable=True))
    op.add_column('invoices', sa.Column('class_comparison', sa.Text(), nullable=True))
    op.add_column('invoices', sa.Column('window_dates', sa.Text(), nullable=True))
    op.add_column('invoices', sa.Column('substantiation_docs', sa.Text(), nullable=True))
    
    # Enhance political_records table
    op.add_column('political_records', sa.Column('election_cycle', sa.String(length=100), nullable=True))
    op.create_index(op.f('ix_political_records_election_cycle'), 'political_records', ['election_cycle'], unique=False)
    op.add_column('political_records', sa.Column('lowest_unit_rate_record', sa.Text(), nullable=True))
    op.add_column('political_records', sa.Column('class_comparison', sa.Text(), nullable=True))
    op.add_column('political_records', sa.Column('window_dates', sa.Text(), nullable=True))
    op.add_column('political_records', sa.Column('substantiation_documentation', sa.Text(), nullable=True))


def downgrade():
    # Remove enhancements from political_records
    op.drop_column('political_records', 'substantiation_documentation')
    op.drop_column('political_records', 'window_dates')
    op.drop_column('political_records', 'class_comparison')
    op.drop_column('political_records', 'lowest_unit_rate_record')
    op.drop_index(op.f('ix_political_records_election_cycle'), table_name='political_records')
    op.drop_column('political_records', 'election_cycle')
    
    # Remove enhancements from invoices
    op.drop_column('invoices', 'substantiation_docs')
    op.drop_column('invoices', 'window_dates')
    op.drop_column('invoices', 'class_comparison')
    op.drop_column('invoices', 'lowest_unit_rate_record')
    op.drop_column('invoices', 'election_cycle')
    op.drop_column('invoices', 'sponsorship_fee')
    op.drop_column('invoices', 'adjustments')
    op.drop_column('invoices', 'credits_issued')
    op.drop_index(op.f('ix_invoices_prepaid'), table_name='invoices')
    op.drop_column('invoices', 'prepaid')
    op.drop_index(op.f('ix_invoices_revenue_bucket'), table_name='invoices')
    op.drop_column('invoices', 'revenue_bucket')
    op.drop_column('invoices', 'gl_account')
    op.drop_column('invoices', 'revenue_class')
    op.drop_index(op.f('ix_invoices_invoice_grouping_code'), table_name='invoices')
    op.drop_column('invoices', 'invoice_grouping_code')
    op.drop_column('invoices', 'billing_notes')
    op.drop_column('invoices', 'billing_schedule')
    op.drop_index(op.f('ix_invoices_billing_end'), table_name='invoices')
    op.drop_column('invoices', 'billing_end')
    op.drop_index(op.f('ix_invoices_billing_start'), table_name='invoices')
    op.drop_column('invoices', 'billing_start')
    
    # Remove enhancements from copy
    op.drop_index(op.f('ix_copy_loudness_check'), table_name='copy')
    op.drop_column('copy', 'loudness_check')
    op.drop_column('copy', 'audio_source')
    op.drop_column('copy', 'audio_format')
    op.drop_column('copy', 'aqh_reconciliation')
    op.drop_column('copy', 'talent_restrictions')
    op.drop_column('copy', 'required_disclaimers')
    op.drop_column('copy', 'legal_copy')
    op.drop_column('copy', 'production_notes')
    op.drop_column('copy', 'priority')
    op.drop_index(op.f('ix_copy_expiration_date'), table_name='copy')
    op.drop_column('copy', 'expiration_date')
    op.drop_index(op.f('ix_copy_effective_date'), table_name='copy')
    op.drop_column('copy', 'effective_date')
    op.drop_column('copy', 'client_provided_audio')
    op.drop_index(op.f('ix_copy_copy_type'), table_name='copy')
    op.drop_column('copy', 'copy_type')
    op.drop_index(op.f('ix_copy_isci_code'), table_name='copy')
    op.drop_column('copy', 'isci_code')
    op.drop_index(op.f('ix_copy_copy_code'), table_name='copy')
    op.drop_column('copy', 'copy_code')
    
    # Drop workflow states table
    op.drop_index(op.f('ix_order_workflow_states_state_changed_at'), table_name='order_workflow_states')
    op.drop_index(op.f('ix_order_workflow_states_state'), table_name='order_workflow_states')
    op.drop_index(op.f('ix_order_workflow_states_order_id'), table_name='order_workflow_states')
    op.drop_table('order_workflow_states')
    
    # Drop revisions table
    op.drop_index(op.f('ix_order_revisions_changed_at'), table_name='order_revisions')
    op.drop_index(op.f('ix_order_revisions_reason_code'), table_name='order_revisions')
    op.drop_index(op.f('ix_order_revisions_revision_number'), table_name='order_revisions')
    op.drop_index(op.f('ix_order_revisions_order_id'), table_name='order_revisions')
    op.drop_table('order_revisions')
    
    # Drop attachments table
    op.drop_index(op.f('ix_order_attachments_uploaded_at'), table_name='order_attachments')
    op.drop_index(op.f('ix_order_attachments_attachment_type'), table_name='order_attachments')
    op.drop_index(op.f('ix_order_attachments_copy_id'), table_name='order_attachments')
    op.drop_index(op.f('ix_order_attachments_order_line_id'), table_name='order_attachments')
    op.drop_index(op.f('ix_order_attachments_order_id'), table_name='order_attachments')
    op.drop_table('order_attachments')
    
    # Drop order_lines table
    op.drop_index(op.f('ix_order_lines_product_code'), table_name='order_lines')
    op.drop_index(op.f('ix_order_lines_preemptible'), table_name='order_lines')
    op.drop_index(op.f('ix_order_lines_guaranteed_position'), table_name='order_lines')
    op.drop_index(op.f('ix_order_lines_makegood_eligible'), table_name='order_lines')
    op.drop_index(op.f('ix_order_lines_priority_code'), table_name='order_lines')
    op.drop_index(op.f('ix_order_lines_daypart'), table_name='order_lines')
    op.drop_index(op.f('ix_order_lines_revenue_type'), table_name='order_lines')
    op.drop_index(op.f('ix_order_lines_station'), table_name='order_lines')
    op.drop_index(op.f('ix_order_lines_end_date'), table_name='order_lines')
    op.drop_index(op.f('ix_order_lines_start_date'), table_name='order_lines')
    op.drop_index(op.f('ix_order_lines_order_id'), table_name='order_lines')
    op.drop_table('order_lines')
    
    # Remove enhancements from orders
    op.drop_constraint('fk_orders_created_by', 'orders', type_='foreignkey')
    op.drop_column('orders', 'created_by')
    op.drop_column('orders', 'revision_number')
    op.drop_index(op.f('ix_orders_locked'), table_name='orders')
    op.drop_column('orders', 'locked')
    op.drop_index(op.f('ix_orders_billing_ready'), table_name='orders')
    op.drop_column('orders', 'billing_ready')
    op.drop_index(op.f('ix_orders_traffic_ready'), table_name='orders')
    op.drop_column('orders', 'traffic_ready')
    op.drop_column('orders', 'required_disclaimers')
    op.drop_column('orders', 'fcc_id')
    op.drop_column('orders', 'regulatory_notes')
    op.drop_index(op.f('ix_orders_insertion_order_number'), table_name='orders')
    op.drop_column('orders', 'insertion_order_number')
    op.drop_index(op.f('ix_orders_contract_reference'), table_name='orders')
    op.drop_column('orders', 'contract_reference')
    op.drop_index(op.f('ix_orders_political_window_flag'), table_name='orders')
    op.drop_column('orders', 'political_window_flag')
    op.drop_index(op.f('ix_orders_political_class'), table_name='orders')
    op.drop_column('orders', 'political_class')
    op.drop_column('orders', 'billing_contact_phone')
    op.drop_column('orders', 'billing_contact_email')
    op.drop_column('orders', 'billing_contact')
    op.drop_column('orders', 'billing_address')
    op.drop_index(op.f('ix_orders_client_po_number'), table_name='orders')
    op.drop_column('orders', 'client_po_number')
    op.drop_column('orders', 'coop_percent')
    op.drop_column('orders', 'coop_sponsor')
    op.drop_column('orders', 'invoice_type')
    op.drop_column('orders', 'billing_cycle')
    op.drop_index(op.f('ix_orders_taxable'), table_name='orders')
    op.drop_column('orders', 'taxable')
    op.drop_column('orders', 'trade_value')
    op.drop_index(op.f('ix_orders_trade_barter'), table_name='orders')
    op.drop_column('orders', 'trade_barter')
    op.drop_column('orders', 'cash_discount')
    op.drop_column('orders', 'agency_discount')
    op.drop_column('orders', 'agency_commission_amount')
    op.drop_column('orders', 'agency_commission_percent')
    op.drop_column('orders', 'net_amount')
    op.drop_column('orders', 'gross_amount')
    op.drop_index(op.f('ix_orders_order_type'), table_name='orders')
    op.drop_column('orders', 'order_type')
    op.drop_column('orders', 'cluster')
    op.drop_column('orders', 'stations')
    op.drop_column('orders', 'sales_region')
    op.drop_column('orders', 'sales_office')
    op.drop_column('orders', 'sales_team')
    op.drop_index(op.f('ix_orders_order_name'), table_name='orders')
    op.drop_column('orders', 'order_name')
    
    # Drop enum types
    op.execute("DROP TYPE IF EXISTS revenuebucket")
    op.execute("DROP TYPE IF EXISTS copytype")
    op.execute("DROP TYPE IF EXISTS workflowstate")
    op.execute("DROP TYPE IF EXISTS revisionreasoncode")
    op.execute("DROP TYPE IF EXISTS attachmenttype")
    op.execute("DROP TYPE IF EXISTS selloutclass")
    op.execute("DROP TYPE IF EXISTS revenuetype")
    op.execute("DROP TYPE IF EXISTS politicalclass")
    op.execute("DROP TYPE IF EXISTS invoicetype")
    op.execute("DROP TYPE IF EXISTS billingcycle")
    op.execute("DROP TYPE IF EXISTS ordertype")

