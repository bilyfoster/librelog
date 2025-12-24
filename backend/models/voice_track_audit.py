"""
Voice track audit model for logging changes
"""

from sqlalchemy import Column, String, DateTime, ForeignKey, text
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class VoiceTrackAudit(Base):
    """Voice track audit model for tracking changes"""
    __tablename__ = "voice_track_audit"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    voice_track_id = Column(UUID(as_uuid=True), ForeignKey("voice_tracks.id"), nullable=False, index=True)
    action = Column(String(50), nullable=False)  # recorded/edited/approved/deleted
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False, index=True)
    timestamp = Column(DateTime(timezone=True), server_default=func.now(), index=True)
    audit_metadata = Column(JSONB, nullable=True)  # Additional details (renamed from metadata - SQLAlchemy reserved)

    # Relationships
    voice_track = relationship("VoiceTrack", backref="audit_logs")
    user = relationship("User", foreign_keys=[user_id])

    def __repr__(self):
        return f"<VoiceTrackAudit(voice_track_id={self.voice_track_id}, action='{self.action}', user_id={self.user_id})>"

