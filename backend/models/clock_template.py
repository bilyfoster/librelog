"""
Clock template model for radio scheduling
"""

from sqlalchemy import Column, String, DateTime, Text, ForeignKey, text
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class ClockTemplate(Base):
    """Clock template model for radio scheduling"""
    __tablename__ = "clock_templates"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    name = Column(String(100), nullable=False, index=True)
    description = Column(Text)
    json_layout = Column(JSONB, nullable=False)
    station_id = Column(UUID(as_uuid=True), ForeignKey("stations.id"), nullable=False, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    station = relationship("Station", back_populates="clock_templates")

    def __repr__(self):
        return f"<ClockTemplate(name='{self.name}', station_id={self.station_id})>"
