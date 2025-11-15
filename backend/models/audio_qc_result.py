"""
AudioQCResult model for storing audio quality control test results
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, Float, Text, ForeignKey
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class AudioQCResult(Base):
    """AudioQCResult model for storing QC test results for audio files"""
    __tablename__ = "audio_qc_results"

    id = Column(Integer, primary_key=True, index=True)
    
    # Association with cut
    cut_id = Column(Integer, ForeignKey("audio_cuts.id"), nullable=False, index=True)
    version_id = Column(Integer, ForeignKey("audio_versions.id"), nullable=True, index=True)
    
    # QC test results stored as JSON
    qc_data = Column(JSONB, nullable=False)  # Comprehensive QC results
    
    # Key metrics (also stored in qc_data, but extracted for easy querying)
    duration_seconds = Column(Float, nullable=True)
    sample_rate = Column(Integer, nullable=True)  # e.g., 44100, 48000
    bitrate = Column(Integer, nullable=True)  # e.g., 128, 192, 320
    channels = Column(Integer, nullable=True)  # 1 = mono, 2 = stereo
    
    # Silence detection
    silence_at_head = Column(Float, nullable=True)  # Seconds of silence at start
    silence_at_tail = Column(Float, nullable=True)  # Seconds of silence at end
    silence_detected = Column(Boolean, default=False, index=True)
    
    # Normalization
    peak_level = Column(Float, nullable=True)  # Peak level in dB
    rms_level = Column(Float, nullable=True)  # RMS level in dB
    lufs_level = Column(Float, nullable=True)  # LUFS level (e.g., -16)
    normalization_applied = Column(Boolean, default=False)
    normalization_method = Column(String(50), nullable=True)  # PEAK, RMS, LUFS
    
    # Clipping detection
    clipping_detected = Column(Boolean, default=False, index=True)
    clipping_samples = Column(Integer, nullable=True)  # Number of clipped samples
    
    # Volume threshold
    volume_threshold_passed = Column(Boolean, default=True)
    minimum_volume = Column(Float, nullable=True)  # Minimum volume level
    
    # Format validation
    format_valid = Column(Boolean, default=True, index=True)
    format_type = Column(String(20), nullable=True)  # MP3, WAV, etc.
    format_errors = Column(Text, nullable=True)  # Format validation errors
    
    # File corruption
    file_corrupted = Column(Boolean, default=False, index=True)
    corruption_details = Column(Text, nullable=True)
    
    # Overall QC status
    qc_passed = Column(Boolean, default=False, index=True)
    qc_warnings = Column(JSONB, nullable=True)  # List of warnings
    qc_errors = Column(JSONB, nullable=True)  # List of errors
    
    # Override information
    overridden = Column(Boolean, default=False)
    overridden_by = Column(Integer, ForeignKey("users.id"), nullable=True)
    override_reason = Column(Text, nullable=True)
    override_timestamp = Column(DateTime(timezone=True), nullable=True)
    
    # Timestamps
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    
    # Relationships
    cut = relationship("AudioCut", foreign_keys=[cut_id])
    version = relationship("AudioVersion", foreign_keys=[version_id])
    override_user = relationship("User", foreign_keys=[overridden_by])
    
    def __repr__(self):
        return f"<AudioQCResult(cut_id={self.cut_id}, passed={self.qc_passed}, overridden={self.overridden})>"

