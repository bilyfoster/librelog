from sqlalchemy import Column, String, Boolean, DateTime, ForeignKey, Text, JSON
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from app.core.database import Base
import uuid


class AdminNotification(Base):
    __tablename__ = "admin_notifications"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    
    # Notification details
    notification_type = Column(String(50), nullable=False)  # rights_change, commercial_use, compensation_due
    title = Column(String(255), nullable=False)
    message = Column(Text, nullable=False)
    priority = Column(String(20), default="medium")  # low, medium, high, urgent
    
    # Related entities
    submission_id = Column(UUID(as_uuid=True), ForeignKey("submissions.id"), nullable=True)
    artist_id = Column(UUID(as_uuid=True), ForeignKey("artists.id"), nullable=True)
    rights_permission_id = Column(UUID(as_uuid=True), ForeignKey("rights_permissions.id"), nullable=True)
    
    # Additional data
    extra_data = Column(JSON, default=dict)  # Additional context data
    
    # Status
    is_read = Column(Boolean, default=False)
    is_resolved = Column(Boolean, default=False)
    
    # Timestamps
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    read_at = Column(DateTime(timezone=True))
    resolved_at = Column(DateTime(timezone=True))
    
    # Relationships
    submission = relationship("Submission")
    artist = relationship("Artist")
    rights_permission = relationship("RightsPermission")


class CommercialUseLog(Base):
    __tablename__ = "commercial_use_logs"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    submission_id = Column(UUID(as_uuid=True), ForeignKey("submissions.id"), nullable=False)
    rights_permission_id = Column(UUID(as_uuid=True), ForeignKey("rights_permissions.id"), nullable=False)
    
    # Commercial use details
    use_type = Column(String(50), nullable=False)  # radio_ad, podcast_ad, event, etc.
    use_description = Column(Text, nullable=False)
    use_date = Column(DateTime(timezone=True), nullable=False)
    
    # Compensation details
    compensation_rate = Column(String(20), nullable=False)  # "per_use", "per_month", "flat_fee"
    compensation_amount = Column(String(20), nullable=False)  # "$50", "$100/month", etc.
    compensation_paid = Column(Boolean, default=False)
    compensation_paid_date = Column(DateTime(timezone=True))
    payment_reference = Column(String(255))  # Check number, transaction ID, etc.
    
    # Additional context
    notes = Column(Text)
    created_by_admin_id = Column(UUID(as_uuid=True), ForeignKey("admin_users.id"))
    
    # Timestamps
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    # Relationships
    submission = relationship("Submission")
    rights_permission = relationship("RightsPermission")
    created_by_admin = relationship("AdminUser")
