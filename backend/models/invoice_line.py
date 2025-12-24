"""
Invoice Line model for billing
"""

from sqlalchemy import Column, Integer, String, DateTime, Numeric, ForeignKey, text
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class InvoiceLine(Base):
    """Invoice Line model for invoice line items"""
    __tablename__ = "invoice_lines"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    invoice_id = Column(UUID(as_uuid=True), ForeignKey("invoices.id"), nullable=False, index=True)
    station_id = Column(UUID(as_uuid=True), ForeignKey("stations.id"), nullable=False, index=True)
    description = Column(String(255), nullable=False)
    quantity = Column(Integer, default=1)
    unit_price = Column(Numeric(10, 2), nullable=False)
    total = Column(Numeric(10, 2), nullable=False)
    spot_ids = Column(JSONB)  # Array of spot IDs included in this line
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    # Relationships
    invoice = relationship("Invoice", back_populates="invoice_lines")
    station = relationship("Station", back_populates="invoice_lines")

    def __repr__(self):
        return f"<InvoiceLine(description='{self.description}', total={self.total})>"

