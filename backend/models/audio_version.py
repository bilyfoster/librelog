"""
AudioVersion model for version history and rollback capability
"""

from sqlalchemy import Column, Integer, String, DateTime, Text, ForeignKey
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class AudioVersion(Base):
    """AudioVersion model for tracking version history of audio cuts"""
    __tablename__ = "audio_versions"

    id = Column(Integer, primary_key=True, index=True)
    cut_id = Column(Integer, ForeignKey("audio_cuts.id"), nullable=False, index=True)
    
    # Version information
    version_number = Column(Integer, nullable=False)
    
    # File information for this version
    audio_file_path = Column(Text, nullable=True)  # Archived file path
    audio_file_url = Column(Text, nullable=True)  # Archived file URL
    file_checksum = Column(String(64), nullable=True)  # MD5 or SHA256 for verification
    
    # Version metadata
    version_notes = Column(Text, nullable=True)  # Notes about this version
    changed_by = Column(Integer, ForeignKey("users.id"), nullable=True)
    
    # Timestamps
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    # Relationships
    cut = relationship("AudioCut", back_populates="versions")
    changer = relationship("User", foreign_keys=[changed_by])
    
    def __repr__(self):
        return f"<AudioVersion(cut_id={self.cut_id}, version={self.version_number})>"

