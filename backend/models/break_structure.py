"""
Break Structure model for traffic scheduling
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, CheckConstraint
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.sql import func
from backend.database import Base


class BreakStructure(Base):
    """Break Structure model for defining break positions by hour"""
    __tablename__ = "break_structures"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(100), nullable=False, index=True)
    hour = Column(Integer, CheckConstraint("hour >= 0 AND hour <= 23"), nullable=False, index=True)
    break_positions = Column(JSONB)  # Array of break times in seconds from hour start
    active = Column(Boolean, default=True, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    def __repr__(self):
        return f"<BreakStructure(name='{self.name}', hour={self.hour})>"

