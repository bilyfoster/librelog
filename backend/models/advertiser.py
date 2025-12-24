"""
Advertiser model for traffic management
"""

from sqlalchemy import Column, String, DateTime, Boolean, Text, Numeric, text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class Advertiser(Base):
    """Advertiser model for managing advertising clients"""
    __tablename__ = "advertisers"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    name = Column(String(255), nullable=False, index=True)
    contact_first_name = Column(String(100))
    contact_last_name = Column(String(100))
    email = Column(String(255), index=True)
    phone = Column(String(50))
    address = Column(Text)
    tax_id = Column(String(50))
    payment_terms = Column(String(100))  # e.g., "Net 30", "Due on receipt"
    credit_limit = Column(Numeric(10, 2))
    active = Column(Boolean, default=True, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    orders = relationship("Order", back_populates="advertiser")
    invoices = relationship("Invoice", back_populates="advertiser")
    copy_items = relationship("Copy", back_populates="advertiser")

    def __repr__(self):
        return f"<Advertiser(name='{self.name}', active={self.active})>"

