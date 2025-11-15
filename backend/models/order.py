"""
Order model for traffic management
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, Date, Numeric, ForeignKey, CheckConstraint, Enum as SQLEnum
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base
import enum


class RateType(str, enum.Enum):
    """Rate type enumeration"""
    ROS = "ROS"  # Run of Schedule
    DAYPART = "DAYPART"  # Daypart-specific
    PROGRAM = "PROGRAM"  # Program-specific
    FIXED_TIME = "FIXED_TIME"  # Fixed time slot


class OrderStatus(str, enum.Enum):
    """Order status enumeration"""
    DRAFT = "DRAFT"
    PENDING = "PENDING"
    APPROVED = "APPROVED"
    ACTIVE = "ACTIVE"
    COMPLETED = "COMPLETED"
    CANCELLED = "CANCELLED"


class ApprovalStatus(str, enum.Enum):
    """Approval status enumeration"""
    PENDING = "PENDING"
    APPROVED = "APPROVED"
    REJECTED = "REJECTED"
    NOT_REQUIRED = "NOT_REQUIRED"


class Order(Base):
    """Order model for managing advertising orders/contracts"""
    __tablename__ = "orders"

    id = Column(Integer, primary_key=True, index=True)
    order_number = Column(String(50), unique=True, nullable=False, index=True)
    campaign_id = Column(Integer, ForeignKey("campaigns.id"), nullable=True, index=True)
    advertiser_id = Column(Integer, ForeignKey("advertisers.id"), nullable=False, index=True)
    agency_id = Column(Integer, ForeignKey("agencies.id"), nullable=True, index=True)
    sales_rep_id = Column(Integer, ForeignKey("sales_reps.id"), nullable=True, index=True)
    start_date = Column(Date, nullable=False, index=True)
    end_date = Column(Date, nullable=False, index=True)
    spot_lengths = Column(JSONB)  # Array of spot lengths in seconds, e.g., [30, 60]
    total_spots = Column(Integer, default=0)
    rate_type = Column(SQLEnum(RateType), nullable=False, default=RateType.ROS)
    rates = Column(JSONB)  # Rate structure based on rate_type
    total_value = Column(Numeric(10, 2), default=0)
    status = Column(SQLEnum(OrderStatus), nullable=False, default=OrderStatus.DRAFT, index=True)
    approval_status = Column(SQLEnum(ApprovalStatus), nullable=False, default=ApprovalStatus.NOT_REQUIRED)
    approved_by = Column(Integer, ForeignKey("users.id"), nullable=True)
    approved_at = Column(DateTime(timezone=True), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    campaign = relationship("Campaign", back_populates="orders")
    advertiser = relationship("Advertiser", back_populates="orders")
    agency = relationship("Agency", back_populates="orders")
    sales_rep = relationship("SalesRep", back_populates="orders")
    spots = relationship("Spot", back_populates="order")
    copy_assignments = relationship("CopyAssignment", back_populates="order")
    copy_items = relationship("Copy", back_populates="order")
    invoices = relationship("Invoice", back_populates="order")
    approver = relationship("User", foreign_keys=[approved_by])

    def __repr__(self):
        return f"<Order(order_number='{self.order_number}', status='{self.status}')>"

