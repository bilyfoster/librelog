"""
Voice track model for voice tracking
"""

from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Text, Boolean, Enum as SQLEnum, Numeric
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base
import enum


class BreakType(str, enum.Enum):
    """Break type enumeration"""
    STANDARD = "standard"
    LEGAL_ID = "legal_id"
    IMAGING = "imaging"


class VoiceTrackStatus(str, enum.Enum):
    """Voice track status enumeration"""
    DRAFT = "draft"
    APPROVED = "approved"
    READY = "ready"
    AIR = "air"


class VoiceTrack(Base):
    """Voice track model for voice tracking"""
    __tablename__ = "voice_tracks"

    id = Column(Integer, primary_key=True, index=True)
    show_name = Column(String(255), index=True)
    file_url = Column(Text, nullable=False)
    scheduled_time = Column(DateTime(timezone=True), index=True)
    uploaded_by = Column(Integer, ForeignKey("users.id"), nullable=False)
    libretime_id = Column(String(50), nullable=True, index=True)  # LibreTime file ID after upload
    standardized_name = Column(String(50), nullable=True, index=True)  # Format: "HH-00_BreakX" for fallback lookup
    recorded_date = Column(DateTime(timezone=True), nullable=True, index=True)  # Date when this recording was made
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    # Extended fields for voice tracking
    break_id = Column(Integer, nullable=True, index=True)  # Reference to break in log
    playlist_id = Column(Integer, nullable=True, index=True)  # Reference to playlist/hourly log
    break_type = Column(SQLEnum(BreakType), nullable=True)
    slot_position = Column(String(10), nullable=True)  # A/B/C/D/E or time offset
    ramp_time = Column(Numeric(5, 2), nullable=True)  # Seconds for intro ramp
    back_time = Column(Numeric(5, 2), nullable=True)  # Seconds for outro ramp
    take_number = Column(Integer, default=1)  # Take number for multiple takes
    is_final = Column(Boolean, default=False, index=True)  # Selected take
    status = Column(SQLEnum(VoiceTrackStatus), default=VoiceTrackStatus.DRAFT, index=True)
    ducking_threshold = Column(Numeric(5, 2), default=-18.0)  # dB level for ducking
    raw_file_url = Column(Text, nullable=True)  # Original recording
    mixed_file_url = Column(Text, nullable=True)  # Ducking/mixed version
    track_metadata = Column(JSONB, nullable=True)  # Timing, markers, etc. (renamed from metadata - SQLAlchemy reserved)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    uploader = relationship("User", back_populates="uploaded_voice_tracks")

    def __repr__(self):
        return f"<VoiceTrack(show_name='{self.show_name}', scheduled_time='{self.scheduled_time}', status='{self.status}')>"
