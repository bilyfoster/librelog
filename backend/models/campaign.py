"""
Campaign model for ad management
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, Date, Text, ForeignKey, Enum as SQLEnum
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base
import enum


class ApprovalStatus(str, enum.Enum):
    """Approval status enumeration"""
    PENDING = "PENDING"
    APPROVED = "APPROVED"
    REJECTED = "REJECTED"
    NOT_REQUIRED = "NOT_REQUIRED"


class RateType(str, enum.Enum):
    """Rate type enumeration"""
    ROS = "ROS"  # Run of Schedule
    DAYPART = "DAYPART"  # Daypart-specific
    PROGRAM = "PROGRAM"  # Program-specific
    FIXED_TIME = "FIXED_TIME"  # Fixed time slot


class Campaign(Base):
    """Campaign model for ad management - extended with order/contract fields"""
    __tablename__ = "campaigns"

    id = Column(Integer, primary_key=True, index=True)
    advertiser = Column(String(255), nullable=False, index=True)  # Keep for backward compatibility
    start_date = Column(Date, nullable=False, index=True)
    end_date = Column(Date, nullable=False, index=True)
    priority = Column(Integer, default=1, index=True)
    file_url = Column(Text)
    active = Column(Boolean, default=True, index=True)
    
    # New order/contract fields
    order_number = Column(String(50), unique=True, nullable=True, index=True)
    contract_number = Column(String(50), nullable=True, index=True)
    insertion_order_url = Column(Text, nullable=True)
    spot_lengths = Column(JSONB)  # Array of spot lengths in seconds
    rate_type = Column(SQLEnum(RateType), nullable=True)
    rates = Column(JSONB)  # Rate structure based on rate_type
    scripts = Column(JSONB)  # Array of script/copy references
    copy_instructions = Column(Text, nullable=True)
    traffic_restrictions = Column(JSONB)  # Restrictions and requirements
    approval_status = Column(SQLEnum(ApprovalStatus), nullable=True, default=ApprovalStatus.NOT_REQUIRED)
    approved_by = Column(Integer, ForeignKey("users.id"), nullable=True)
    approved_at = Column(DateTime(timezone=True), nullable=True)
    
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    orders = relationship("Order", back_populates="campaign")
    spots = relationship("Spot", back_populates="campaign")
    makegoods = relationship("Makegood", back_populates="campaign")
    invoices = relationship("Invoice", back_populates="campaign")
    approver_user = relationship("User", foreign_keys=[approved_by])

    def __repr__(self):
        return f"<Campaign(advertiser='{self.advertiser}', priority={self.priority})>"
