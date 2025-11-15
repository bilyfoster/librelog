"""
Daypart model for traffic scheduling
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, Time, ForeignKey
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class Daypart(Base):
    """Daypart model for defining time-based scheduling rules"""
    __tablename__ = "dayparts"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(100), nullable=False, index=True)
    start_time = Column(Time, nullable=False)
    end_time = Column(Time, nullable=False)
    days_of_week = Column(JSONB)  # Array of day numbers (0=Monday, 6=Sunday)
    category_id = Column(Integer, ForeignKey("daypart_categories.id"), nullable=True, index=True)
    description = Column(String(255), nullable=True)
    active = Column(Boolean, default=True, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    
    # Relationships
    category = relationship("DaypartCategory", foreign_keys=[category_id])

    def __repr__(self):
        return f"<Daypart(name='{self.name}', start_time='{self.start_time}', end_time='{self.end_time}')>"

