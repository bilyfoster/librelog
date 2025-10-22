"""
Daily log model for broadcast scheduling
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, Date, ForeignKey
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class DailyLog(Base):
    """Daily log model for broadcast scheduling"""
    __tablename__ = "daily_logs"

    id = Column(Integer, primary_key=True, index=True)
    date = Column(Date, nullable=False, index=True)
    generated_by = Column(Integer, ForeignKey("users.id"), nullable=False)
    json_data = Column(JSONB, nullable=False)
    published = Column(Boolean, default=False, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    generator = relationship("User", back_populates="generated_logs")
    playback_history = relationship("PlaybackHistory", back_populates="log")

    def __repr__(self):
        return f"<DailyLog(date='{self.date}', published={self.published})>"
