"""
Sales Team model for traffic management
"""

from sqlalchemy import Column, String, DateTime, Boolean, Text, text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class SalesTeam(Base):
    """Sales Team model for managing sales teams"""
    __tablename__ = "sales_teams"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    name = Column(String(255), nullable=False, unique=True, index=True)
    description = Column(Text, nullable=True)
    active = Column(Boolean, default=True, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    sales_reps = relationship("SalesRep", secondary="sales_rep_teams", back_populates="teams")
    orders = relationship("Order", back_populates="sales_team_rel")

    def __repr__(self):
        return f"<SalesTeam(name='{self.name}', active={self.active})>"

