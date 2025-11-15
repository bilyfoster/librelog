"""
Track model for music library management
"""

from sqlalchemy import Column, Integer, String, DateTime, CheckConstraint, Text
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class Track(Base):
    """Track model for music library with metadata"""
    __tablename__ = "tracks"

    id = Column(Integer, primary_key=True, index=True)
    title = Column(String(255), nullable=False, index=True)
    artist = Column(String(255), index=True)
    album = Column(String(255))
    type = Column(
        String(10),
        CheckConstraint("type IN ('MUS', 'ADV', 'PSA', 'LIN', 'INT', 'PRO', 'SHO', 'IDS', 'COM', 'NEW')"),
        nullable=False,
        index=True
    )
    genre = Column(String(100), index=True)
    duration = Column(Integer)  # Duration in seconds
    filepath = Column(Text, nullable=False)
    libretime_id = Column(String(50), unique=True, index=True)  # LibreTime track ID
    last_played = Column(DateTime(timezone=True))
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    playback_history = relationship("PlaybackHistory", back_populates="track")

    def __repr__(self):
        return f"<Track(title='{self.title}', artist='{self.artist}', type='{self.type}')>"
