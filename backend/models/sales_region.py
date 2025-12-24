"""
Sales Region model for traffic management
"""

from sqlalchemy import Column, String, DateTime, Boolean, Text, text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class SalesRegion(Base):
    """Sales Region model for managing sales regions"""
    __tablename__ = "sales_regions"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    name = Column(String(255), nullable=False, unique=True, index=True)
    description = Column(Text, nullable=True)
    active = Column(Boolean, default=True, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    sales_offices = relationship("SalesOffice", back_populates="region")
    sales_reps = relationship("SalesRep", secondary="sales_rep_regions", back_populates="regions")
    orders = relationship("Order", back_populates="sales_region_rel")

    def __repr__(self):
        return f"<SalesRegion(name='{self.name}', active={self.active})>"

