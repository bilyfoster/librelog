"""
Playback history model for reconciliation
"""

from sqlalchemy import Column, Integer, DateTime, ForeignKey
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class PlaybackHistory(Base):
    """Playback history model for reconciliation"""
    __tablename__ = "playback_history"

    id = Column(Integer, primary_key=True, index=True)
    track_id = Column(Integer, ForeignKey("tracks.id"), nullable=False)
    log_id = Column(Integer, ForeignKey("daily_logs.id"), nullable=False)
    played_at = Column(DateTime(timezone=True), server_default=func.now(), index=True)
    duration_played = Column(Integer)  # Duration in seconds

    # Relationships
    track = relationship("Track", back_populates="playback_history")
    log = relationship("DailyLog", back_populates="playback_history")

    def __repr__(self):
        return f"<PlaybackHistory(track_id={self.track_id}, played_at='{self.played_at}')>"
