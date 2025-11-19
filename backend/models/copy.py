"""
Copy model for audio asset and script management
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, Text, ForeignKey, Enum as SQLEnum
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base
import enum


class CopyStatus(str, enum.Enum):
    """Copy status enumeration"""
    DRAFT = "draft"
    PENDING_APPROVAL = "pending_approval"
    APPROVED = "approved"
    IN_PRODUCTION = "in_production"
    COMPLETED = "completed"
    DELIVERED = "delivered"


class CopyApprovalStatus(str, enum.Enum):
    """Copy approval status enumeration"""
    PENDING = "pending"
    APPROVED = "approved"
    REJECTED = "rejected"


class Copy(Base):
    """Copy model for managing audio assets and scripts"""
    __tablename__ = "copy"

    id = Column(Integer, primary_key=True, index=True)
    order_id = Column(Integer, ForeignKey("orders.id"), nullable=True, index=True)
    advertiser_id = Column(Integer, ForeignKey("advertisers.id"), nullable=True, index=True)
    title = Column(String(255), nullable=False, index=True)
    script_text = Column(Text, nullable=True)
    audio_file_path = Column(Text, nullable=True)
    audio_file_url = Column(Text, nullable=True)
    version = Column(Integer, default=1)
    expires_at = Column(DateTime(timezone=True), nullable=True, index=True)
    active = Column(Boolean, default=True, index=True)
    
    # Multi-cut support
    cut_count = Column(Integer, default=0)  # Number of cuts for this copy
    rotation_mode = Column(String(50), nullable=True)  # SEQUENTIAL, RANDOM, WEIGHTED, DAYPART, PROGRAM
    
    # Political and live read flags
    political_flag = Column(Boolean, default=False, index=True)
    live_read_enabled = Column(Boolean, default=False, index=True)
    
    # Copy instructions
    copy_instructions = Column(Text, nullable=True)  # Instructions for talent
    instruction_attachments = Column(Text, nullable=True)  # JSON array of PDF/image paths
    
    # Production workflow fields
    production_order_id = Column(Integer, ForeignKey("production_orders.id"), nullable=True, unique=True, index=True)
    copy_status = Column(SQLEnum(CopyStatus), nullable=False, default=CopyStatus.DRAFT, index=True)
    needs_production = Column(Boolean, default=False, index=True)  # KEY FLAG: When true + copy approved → auto-create ProductionOrder
    copy_approval_status = Column(SQLEnum(CopyApprovalStatus), nullable=False, default=CopyApprovalStatus.PENDING, index=True)
    script_approved_at = Column(DateTime(timezone=True), nullable=True)
    script_approved_by = Column(Integer, ForeignKey("users.id"), nullable=True)
    
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    order = relationship("Order", back_populates="copy_items")
    advertiser = relationship("Advertiser", back_populates="copy_items")
    copy_assignments = relationship("CopyAssignment", back_populates="copy")
    audio_cuts = relationship("AudioCut", back_populates="copy", cascade="all, delete-orphan")
    production_order = relationship("ProductionOrder", back_populates="copy", uselist=False, foreign_keys=[production_order_id])
    script_approver = relationship("User", foreign_keys=[script_approved_by])

    def __repr__(self):
        return f"<Copy(title='{self.title}', version={self.version})>"

