"""
Agency model for traffic management
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, Text, Numeric
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class Agency(Base):
    """Agency model for managing advertising agencies"""
    __tablename__ = "agencies"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(255), nullable=False, index=True)
    contact_name = Column(String(255))
    email = Column(String(255), index=True)
    phone = Column(String(50))
    address = Column(Text)
    commission_rate = Column(Numeric(5, 2))  # Percentage, e.g., 15.00 for 15%
    active = Column(Boolean, default=True, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    orders = relationship("Order", back_populates="agency")
    invoices = relationship("Invoice", back_populates="agency")

    def __repr__(self):
        return f"<Agency(name='{self.name}', active={self.active})>"

