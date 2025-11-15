"""
AudioCut model for multi-cut audio asset management
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, Text, ForeignKey, Float, CheckConstraint
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class AudioCut(Base):
    """AudioCut model for managing multiple cuts per copy"""
    __tablename__ = "audio_cuts"

    id = Column(Integer, primary_key=True, index=True)
    copy_id = Column(Integer, ForeignKey("copy.id"), nullable=False, index=True)
    
    # Cut identification
    cut_id = Column(String(50), nullable=False, index=True)  # A, B, C, 1, 2, 3, or custom
    cut_name = Column(String(255), nullable=True)  # Optional descriptive name
    
    # Audio file information
    audio_file_path = Column(Text, nullable=True)
    audio_file_url = Column(Text, nullable=True)
    file_checksum = Column(String(64), nullable=True)  # MD5 or SHA256
    
    # Versioning
    version = Column(Integer, default=1, nullable=False)
    
    # Rotation configuration
    rotation_weight = Column(Float, default=1.0)  # Weight for weighted rotation (e.g., 0.7 for 70%)
    daypart_restrictions = Column(JSONB, nullable=True)  # List of daypart IDs where this cut can play
    program_associations = Column(JSONB, nullable=True)  # List of program IDs where this cut should play
    
    # Expiration and activation
    expires_at = Column(DateTime(timezone=True), nullable=True, index=True)
    active = Column(Boolean, default=True, index=True)
    
    # QC results stored as JSON
    qc_results = Column(JSONB, nullable=True)  # Store QC test results
    
    # Metadata
    notes = Column(Text, nullable=True)
    tags = Column(JSONB, nullable=True)  # List of tags for searching
    created_by = Column(Integer, ForeignKey("users.id"), nullable=True)
    
    # Timestamps
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    
    # Relationships
    copy = relationship("Copy", back_populates="audio_cuts")
    creator = relationship("User", foreign_keys=[created_by])
    versions = relationship("AudioVersion", back_populates="cut", cascade="all, delete-orphan")
    
    # Constraints
    __table_args__ = (
        CheckConstraint('rotation_weight >= 0', name='check_rotation_weight_positive'),
    )
    
    def __repr__(self):
        return f"<AudioCut(cut_id='{self.cut_id}', version={self.version}, copy_id={self.copy_id})>"

