"""
Payment model for billing
"""

from sqlalchemy import Column, String, DateTime, Numeric, ForeignKey, text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class Payment(Base):
    """Payment model for tracking invoice payments"""
    __tablename__ = "payments"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    invoice_id = Column(UUID(as_uuid=True), ForeignKey("invoices.id"), nullable=False, index=True)
    amount = Column(Numeric(10, 2), nullable=False)
    payment_date = Column(DateTime(timezone=True), nullable=False, index=True)
    payment_method = Column(String(50))  # e.g., "Check", "Credit Card", "Wire Transfer"
    reference_number = Column(String(100))
    notes = Column(String(500))
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    # Relationships
    invoice = relationship("Invoice", back_populates="payments")

    def __repr__(self):
        return f"<Payment(invoice_id={self.invoice_id}, amount={self.amount})>"

