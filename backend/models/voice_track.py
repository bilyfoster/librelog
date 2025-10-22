"""
Voice track model for voice tracking
"""

from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Text
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class VoiceTrack(Base):
    """Voice track model for voice tracking"""
    __tablename__ = "voice_tracks"

    id = Column(Integer, primary_key=True, index=True)
    show_name = Column(String(255), index=True)
    file_url = Column(Text, nullable=False)
    scheduled_time = Column(DateTime(timezone=True), index=True)
    uploaded_by = Column(Integer, ForeignKey("users.id"), nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    # Relationships
    uploader = relationship("User", back_populates="uploaded_voice_tracks")

    def __repr__(self):
        return f"<VoiceTrack(show_name='{self.show_name}', scheduled_time='{self.scheduled_time}')>"
