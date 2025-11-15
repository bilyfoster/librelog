"""
AudioDelivery model for tracking delivery to playback systems
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, Text, ForeignKey
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base
import enum


class DeliveryMethod(str, enum.Enum):
    """Delivery method enumeration"""
    SFTP = "SFTP"
    RSYNC = "RSYNC"
    API = "API"
    LOCAL = "LOCAL"


class DeliveryStatus(str, enum.Enum):
    """Delivery status enumeration"""
    PENDING = "pending"
    IN_PROGRESS = "in_progress"
    SUCCESS = "success"
    FAILED = "failed"
    RETRYING = "retrying"


class AudioDelivery(Base):
    """AudioDelivery model for tracking audio file delivery to playback systems"""
    __tablename__ = "audio_deliveries"

    id = Column(Integer, primary_key=True, index=True)
    
    # Association with cut
    cut_id = Column(Integer, ForeignKey("audio_cuts.id"), nullable=False, index=True)
    copy_id = Column(Integer, ForeignKey("copy.id"), nullable=True, index=True)
    
    # Delivery configuration
    delivery_method = Column(String(20), nullable=False)  # SFTP, RSYNC, API, LOCAL
    target_server = Column(String(255), nullable=False)  # Server hostname or IP
    target_path = Column(Text, nullable=True)  # Destination path on target server
    
    # Delivery status
    status = Column(String(20), default=DeliveryStatus.PENDING.value, index=True)
    retry_count = Column(Integer, default=0)
    max_retries = Column(Integer, default=3)
    
    # Verification
    source_checksum = Column(String(64), nullable=True)  # Checksum of source file
    delivered_checksum = Column(String(64), nullable=True)  # Checksum of delivered file
    checksum_verified = Column(Boolean, default=False)
    
    # Timestamps
    delivery_started_at = Column(DateTime(timezone=True), nullable=True)
    delivery_completed_at = Column(DateTime(timezone=True), nullable=True)
    last_retry_at = Column(DateTime(timezone=True), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    
    # Error tracking
    error_message = Column(Text, nullable=True)
    error_details = Column(Text, nullable=True)  # JSON error details
    
    # Relationships
    cut = relationship("AudioCut", foreign_keys=[cut_id])
    copy = relationship("Copy", foreign_keys=[copy_id])
    
    def __repr__(self):
        return f"<AudioDelivery(cut_id={self.cut_id}, method='{self.delivery_method}', status='{self.status}')>"

