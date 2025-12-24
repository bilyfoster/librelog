"""
Daypart model for traffic scheduling
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, Time, ForeignKey, text
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class Daypart(Base):
    """Daypart model for defining time-based scheduling rules"""
    __tablename__ = "dayparts"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    name = Column(String(100), nullable=False, index=True)
    start_time = Column(Time, nullable=False)
    end_time = Column(Time, nullable=False)
    days_of_week = Column(JSONB)  # Array of day numbers (0=Monday, 6=Sunday)
    category_id = Column(UUID(as_uuid=True), ForeignKey("daypart_categories.id"), nullable=True, index=True)
    station_id = Column(UUID(as_uuid=True), ForeignKey("stations.id"), nullable=False, index=True)
    description = Column(String(255), nullable=True)
    active = Column(Boolean, default=True, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    
    # Relationships
    category = relationship("DaypartCategory", foreign_keys=[category_id])
    station = relationship("Station", back_populates="dayparts")

    def __repr__(self):
        return f"<Daypart(name='{self.name}', start_time='{self.start_time}', end_time='{self.end_time}')>"

