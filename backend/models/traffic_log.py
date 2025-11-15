"""
Traffic Log model for tracking traffic operations
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, ForeignKey, Text, Enum as SQLEnum
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base
import enum


class TrafficLogType(str, enum.Enum):
    """Traffic log type enumeration"""
    SPOT_SCHEDULED = "SPOT_SCHEDULED"
    SPOT_MOVED = "SPOT_MOVED"
    SPOT_DELETED = "SPOT_DELETED"
    LOG_LOCKED = "LOG_LOCKED"
    LOG_UNLOCKED = "LOG_UNLOCKED"
    LOG_PUBLISHED = "LOG_PUBLISHED"
    CONFLICT_DETECTED = "CONFLICT_DETECTED"
    CONFLICT_RESOLVED = "CONFLICT_RESOLVED"
    COPY_ASSIGNED = "COPY_ASSIGNED"
    COPY_UNASSIGNED = "COPY_UNASSIGNED"
    ORDER_CREATED = "ORDER_CREATED"
    ORDER_UPDATED = "ORDER_UPDATED"
    ORDER_APPROVED = "ORDER_APPROVED"
    ORDER_CANCELLED = "ORDER_CANCELLED"


class TrafficLog(Base):
    """Traffic Log model for tracking all traffic operations"""
    __tablename__ = "traffic_logs"

    id = Column(Integer, primary_key=True, index=True)
    log_type = Column(SQLEnum(TrafficLogType), nullable=False, index=True)
    
    # Related entities
    log_id = Column(Integer, ForeignKey("daily_logs.id"), nullable=True, index=True)
    spot_id = Column(Integer, ForeignKey("spots.id"), nullable=True, index=True)
    order_id = Column(Integer, ForeignKey("orders.id"), nullable=True, index=True)
    campaign_id = Column(Integer, ForeignKey("campaigns.id"), nullable=True, index=True)
    copy_id = Column(Integer, ForeignKey("copy.id"), nullable=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    
    # Log details
    message = Column(Text, nullable=False)
    meta_data = Column(JSONB, nullable=True)  # Additional context data (renamed from 'metadata' to avoid SQLAlchemy conflict)
    ip_address = Column(String(45), nullable=True)
    user_agent = Column(Text, nullable=True)
    
    # Timestamp
    created_at = Column(DateTime(timezone=True), server_default=func.now(), index=True)
    
    # Relationships
    log = relationship("DailyLog", foreign_keys=[log_id])
    spot = relationship("Spot", foreign_keys=[spot_id])
    order = relationship("Order", foreign_keys=[order_id])
    campaign = relationship("Campaign", foreign_keys=[campaign_id])
    copy = relationship("Copy", foreign_keys=[copy_id])
    user = relationship("User", foreign_keys=[user_id])

    def __repr__(self):
        return f"<TrafficLog(type='{self.log_type}', message='{self.message[:50]}...')>"

