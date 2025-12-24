"""
Sales Office model for traffic management
"""

from sqlalchemy import Column, String, DateTime, Boolean, Text, ForeignKey, text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class SalesOffice(Base):
    """Sales Office model for managing sales offices"""
    __tablename__ = "sales_offices"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    name = Column(String(255), nullable=False, index=True)
    address = Column(Text, nullable=True)
    region_id = Column(UUID(as_uuid=True), ForeignKey("sales_regions.id"), nullable=True, index=True)
    active = Column(Boolean, default=True, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    region = relationship("SalesRegion", back_populates="sales_offices")
    sales_reps = relationship("SalesRep", secondary="sales_rep_offices", back_populates="offices")
    orders = relationship("Order", back_populates="sales_office_rel")

    def __repr__(self):
        return f"<SalesOffice(name='{self.name}', region_id={self.region_id}, active={self.active})>"

