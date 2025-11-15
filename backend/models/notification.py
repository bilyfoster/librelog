"""
Notification model for email and in-app notifications
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, ForeignKey, Text, Enum as SQLEnum
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base
import enum


class NotificationType(str, enum.Enum):
    """Notification type enumeration"""
    EMAIL = "EMAIL"
    IN_APP = "IN_APP"
    BOTH = "BOTH"


class NotificationStatus(str, enum.Enum):
    """Notification status enumeration"""
    PENDING = "PENDING"
    SENT = "SENT"
    FAILED = "FAILED"
    READ = "READ"


class Notification(Base):
    """Notification model for user notifications"""
    __tablename__ = "notifications"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    notification_type = Column(SQLEnum(NotificationType), nullable=False, default=NotificationType.IN_APP)
    status = Column(SQLEnum(NotificationStatus), nullable=False, default=NotificationStatus.PENDING, index=True)
    subject = Column(String(255), nullable=True)
    message = Column(Text, nullable=False)
    meta_data = Column(JSONB, nullable=True)  # Additional data (event type, resource ID, etc.) (renamed from 'metadata' to avoid SQLAlchemy conflict)
    read_at = Column(DateTime(timezone=True), nullable=True)
    sent_at = Column(DateTime(timezone=True), nullable=True)
    error_message = Column(Text, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), index=True)

    # Relationships
    user = relationship("User", backref="notifications")

    def __repr__(self):
        return f"<Notification(user_id={self.user_id}, type='{self.notification_type}', status='{self.status}')>"

