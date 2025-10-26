from sqlalchemy import Column, String, Text, Boolean, DateTime, Integer, Numeric, ForeignKey, Index
from sqlalchemy.dialects.postgresql import UUID, JSON
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from app.core.database import Base
import uuid


class Submission(Base):
    __tablename__ = "submissions"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    tracking_id = Column(String(20), unique=True, nullable=False, index=True)
    artist_id = Column(UUID(as_uuid=True), ForeignKey("artists.id", ondelete="CASCADE"), nullable=False, index=True)
    artist_name = Column(String(255), nullable=False)  # Store artist name at time of submission
    song_title = Column(String(255), nullable=False)
    genre = Column(String(100))
    file_name = Column(String(255), nullable=False)
    file_size = Column(Integer, nullable=False)
    file_path = Column(String(500), nullable=False)
    file_status = Column(String(20), default="uploading")  # uploading, processing, ready, error
    duration_seconds = Column(Integer)
    bitrate = Column(Integer)
    sample_rate = Column(Integer)
    channels = Column(Integer)
    lufs_reading = Column(Numeric(5, 2))
    true_peak = Column(Numeric(5, 2))
    status = Column(String(20), default="pending", index=True)  # pending, approved, rejected, needs_info, isrc_assigned
    isrc_requested = Column(Boolean, default=False, index=True)
    radio_permission = Column(Boolean, default=False)
    public_display = Column(Boolean, default=False)
    rights_attestation = Column(Boolean, default=False)
    pro_info = Column(JSON, default=dict)
    admin_notes = Column(Text)
    submitted_at = Column(DateTime(timezone=True), server_default=func.now(), index=True)
    reviewed_at = Column(DateTime(timezone=True))
    reviewed_by = Column(UUID(as_uuid=True), ForeignKey("admin_users.id"))
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    artist = relationship("Artist", back_populates="submissions")
    isrc = relationship("ISRC", back_populates="submission", uselist=False)
    collaborators = relationship("Collaborator", back_populates="submission", cascade="all, delete-orphan")
    rights_permission = relationship("RightsPermission", back_populates="submission", uselist=False, cascade="all, delete-orphan")

    # Indexes
    __table_args__ = (
        Index('idx_submissions_artist_status', 'artist_id', 'status'),
        Index('idx_submissions_submitted_at_status', 'submitted_at', 'status'),
    )
