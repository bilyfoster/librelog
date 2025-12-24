"""
Daily log model for broadcast scheduling
"""

from sqlalchemy import Column, String, DateTime, Boolean, Date, ForeignKey, UniqueConstraint, text
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class DailyLog(Base):
    """Daily log model for broadcast scheduling"""
    __tablename__ = "daily_logs"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    date = Column(Date, nullable=False, index=True)
    station_id = Column(UUID(as_uuid=True), ForeignKey("stations.id"), nullable=False, index=True)
    generated_by = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)
    json_data = Column(JSONB, nullable=False)
    published = Column(Boolean, default=False, index=True)
    locked = Column(Boolean, default=False, index=True)
    locked_at = Column(DateTime(timezone=True), nullable=True)
    locked_by = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=True)
    conflicts = Column(JSONB)  # Array of conflict objects
    oversell_warnings = Column(JSONB)  # Array of oversell warning objects
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    
    __table_args__ = (
        # Unique constraint: one log per station per day
        UniqueConstraint('date', 'station_id', name='uq_daily_logs_date_station'),
    )

    # Relationships
    station = relationship("Station", back_populates="daily_logs")
    generator = relationship("User", foreign_keys=[generated_by], back_populates="generated_logs")
    locker = relationship("User", foreign_keys=[locked_by])
    playback_history = relationship("PlaybackHistory", back_populates="log")
    revisions = relationship("LogRevision", back_populates="log")

    def __repr__(self):
        return f"<DailyLog(date='{self.date}', station_id={self.station_id}, published={self.published})>"
