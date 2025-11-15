"""
Invoice model for billing
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, Date, Numeric, ForeignKey, Enum as SQLEnum
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


class Invoice(Base):
    """Invoice model for billing"""
    __tablename__ = "invoices"

    id = Column(Integer, primary_key=True, index=True)
    invoice_number = Column(String(50), unique=True, nullable=False, index=True)
    advertiser_id = Column(Integer, ForeignKey("advertisers.id"), nullable=False, index=True)
    agency_id = Column(Integer, ForeignKey("agencies.id"), nullable=True, index=True)
    order_id = Column(Integer, ForeignKey("orders.id"), nullable=True, index=True)
    campaign_id = Column(Integer, ForeignKey("campaigns.id"), nullable=True, index=True)
    invoice_date = Column(Date, nullable=False, index=True)
    due_date = Column(Date, nullable=False, index=True)
    subtotal = Column(Numeric(10, 2), default=0)
    tax = Column(Numeric(10, 2), default=0)
    total = Column(Numeric(10, 2), default=0)
    status = Column(SQLEnum(InvoiceStatus), nullable=False, default=InvoiceStatus.DRAFT, index=True)
    payment_terms = Column(String(100))
    notes = Column(String(500))
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

