"""
Invoice model for billing
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, Date, Numeric, ForeignKey, Enum as SQLEnum, Text, text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base
import enum


class InvoiceStatus(str, enum.Enum):
    """Invoice status enumeration"""
    DRAFT = "DRAFT"
    SENT = "SENT"
    PAID = "PAID"
    OVERDUE = "OVERDUE"
    CANCELLED = "CANCELLED"


class RevenueBucket(str, enum.Enum):
    """Revenue bucket enumeration - WideOrbit compatible"""
    SPOT = "SPOT"
    DIGITAL = "DIGITAL"
    TRADE = "TRADE"
    PROMOTIONS = "PROMOTIONS"


class Invoice(Base):
    """Invoice model for billing"""
    __tablename__ = "invoices"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    invoice_number = Column(String(50), unique=True, nullable=False, index=True)
    advertiser_id = Column(UUID(as_uuid=True), ForeignKey("advertisers.id"), nullable=False, index=True)
    agency_id = Column(UUID(as_uuid=True), ForeignKey("agencies.id"), nullable=True, index=True)
    order_id = Column(UUID(as_uuid=True), ForeignKey("orders.id"), nullable=True, index=True)
    campaign_id = Column(UUID(as_uuid=True), ForeignKey("campaigns.id"), nullable=True, index=True)
    invoice_date = Column(Date, nullable=False, index=True)
    due_date = Column(Date, nullable=False, index=True)
    subtotal = Column(Numeric(10, 2), default=0)
    tax = Column(Numeric(10, 2), default=0)
    total = Column(Numeric(10, 2), default=0)
    status = Column(SQLEnum(InvoiceStatus), nullable=False, default=InvoiceStatus.DRAFT, index=True)
    payment_terms = Column(String(100))
    notes = Column(String(500))
    
    # WideOrbit Billing Fields
    billing_start = Column(Date, nullable=True, index=True)
    billing_end = Column(Date, nullable=True, index=True)
    billing_schedule = Column(Text, nullable=True)
    billing_notes = Column(Text, nullable=True)
    invoice_grouping_code = Column(String(100), nullable=True, index=True)
    revenue_class = Column(String(100), nullable=True)
    gl_account = Column(String(100), nullable=True)  # GL Account for accounting export
    revenue_bucket = Column(SQLEnum(RevenueBucket), nullable=True, index=True)
    prepaid = Column(Boolean, default=False, index=True)
    credits_issued = Column(Numeric(10, 2), default=0)
    adjustments = Column(Numeric(10, 2), default=0)
    sponsorship_fee = Column(Numeric(10, 2), nullable=True)
    
    # Political Compliance Fields
    election_cycle = Column(String(100), nullable=True)
    lowest_unit_rate_record = Column(Text, nullable=True)
    class_comparison = Column(Text, nullable=True)
    window_dates = Column(Text, nullable=True)
    substantiation_docs = Column(Text, nullable=True)  # References to documentation
    
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    advertiser = relationship("Advertiser", back_populates="invoices")
    agency = relationship("Agency", back_populates="invoices")
    order = relationship("Order", back_populates="invoices")
    campaign = relationship("Campaign", back_populates="invoices")
    invoice_lines = relationship("InvoiceLine", back_populates="invoice", cascade="all, delete-orphan")
    payments = relationship("Payment", back_populates="invoice")

    def __repr__(self):
        return f"<Invoice(invoice_number='{self.invoice_number}', status='{self.status}')>"

