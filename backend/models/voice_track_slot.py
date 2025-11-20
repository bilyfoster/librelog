"""
Voice track slot model for break assignments
"""

from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Numeric
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class VoiceTrackSlot(Base):
    """Voice track slot model for break assignments"""
    __tablename__ = "voice_track_slots"

    id = Column(Integer, primary_key=True, index=True)
    log_id = Column(Integer, ForeignKey("daily_logs.id"), nullable=False, index=True)
    hour = Column(Integer, nullable=False, index=True)  # 0-23
    break_position = Column(String(10), nullable=True)  # A/B/C/D/E or time offset
    standardized_name = Column(String(50), nullable=True, index=True)  # Format: "HH-00_BreakX" for fallback lookup
    assigned_dj_id = Column(Integer, ForeignKey("users.id"), nullable=True, index=True)
    voice_track_id = Column(Integer, ForeignKey("voice_tracks.id"), nullable=True, index=True)
    previous_track_id = Column(Integer, ForeignKey("tracks.id"), nullable=True)  # For preview
    next_track_id = Column(Integer, ForeignKey("tracks.id"), nullable=True)  # For preview
    ramp_time = Column(Numeric(5, 2), nullable=True)  # Calculated from track metadata
    status = Column(String(20), default="pending", index=True)  # pending/assigned/recorded/approved
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    log = relationship("DailyLog", backref="voice_track_slots")
    assigned_dj = relationship("User", foreign_keys=[assigned_dj_id])
    voice_track = relationship("VoiceTrack", foreign_keys=[voice_track_id])
    previous_track = relationship("Track", foreign_keys=[previous_track_id])
    next_track = relationship("Track", foreign_keys=[next_track_id])

    def __repr__(self):
        return f"<VoiceTrackSlot(log_id={self.log_id}, hour={self.hour}, break_position='{self.break_position}', status='{self.status}')>"

