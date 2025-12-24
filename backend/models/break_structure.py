"""
Break Structure model for traffic scheduling
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, CheckConstraint, ForeignKey, text
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class BreakStructure(Base):
    """Break Structure model for defining break positions by hour"""
    __tablename__ = "break_structures"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    name = Column(String(100), nullable=False, index=True)
    hour = Column(Integer, CheckConstraint("hour >= 0 AND hour <= 23"), nullable=False, index=True)
    break_positions = Column(JSONB)  # Array of break times in seconds from hour start
    station_id = Column(UUID(as_uuid=True), ForeignKey("stations.id"), nullable=False, index=True)
    active = Column(Boolean, default=True, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    station = relationship("Station", back_populates="break_structures")

    def __repr__(self):
        return f"<BreakStructure(name='{self.name}', hour={self.hour}, station_id={self.station_id})>"

