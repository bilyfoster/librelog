from sqlalchemy import Column, String, Boolean, DateTime, ForeignKey, Text, JSON, Integer, Numeric
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from app.core.database import Base
import uuid


class RightsPermission(Base):
    __tablename__ = "rights_permissions"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    submission_id = Column(UUID(as_uuid=True), ForeignKey("submissions.id", ondelete="CASCADE"), nullable=False)
    
    # Radio broadcast rights
    radio_play_permission = Column(Boolean, default=False)
    radio_play_granted_at = Column(DateTime(timezone=True))
    radio_play_revoked_at = Column(DateTime(timezone=True))
    radio_play_terms = Column(Text)  # Custom terms for radio play
    
    # Public display rights
    public_display_permission = Column(Boolean, default=False)
    public_display_granted_at = Column(DateTime(timezone=True))
    public_display_revoked_at = Column(DateTime(timezone=True))
    
    # Podcast rights (replaces streaming)
    podcast_permission = Column(Boolean, default=False)
    podcast_granted_at = Column(DateTime(timezone=True))
    podcast_revoked_at = Column(DateTime(timezone=True))
    
    # Commercial use rights
    commercial_use_permission = Column(Boolean, default=False)
    commercial_use_granted_at = Column(DateTime(timezone=True))
    commercial_use_revoked_at = Column(DateTime(timezone=True))
    
    # Commercial compensation tracking
    commercial_compensation_rate = Column(Numeric(10, 2))  # Rate per use (e.g., $50 per commercial)
    commercial_compensation_paid = Column(Numeric(10, 2), default=0)  # Total compensation paid
    commercial_uses_count = Column(Integer, default=0)  # Number of commercial uses
    last_commercial_use = Column(DateTime(timezone=True))  # Last commercial use date
    
    # Rights holder information
    rights_holder_name = Column(String(255))
    rights_holder_email = Column(String(255))
    rights_holder_phone = Column(String(50))
    
    # Legal information
    copyright_year = Column(String(4))
    copyright_owner = Column(String(255))
    publisher = Column(String(255))
    label = Column(String(255))
    
    # Additional terms and conditions
    custom_terms = Column(Text)
    restrictions = Column(Text)
    
    # Audit fields
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    last_modified_by = Column(UUID(as_uuid=True), ForeignKey("artists.id"))
    
    # Relationships
    submission = relationship("Submission", back_populates="rights_permission")
    last_modified_artist = relationship("Artist", foreign_keys=[last_modified_by])


# Create a separate table for tracking permission changes
class RightsPermissionHistory(Base):
    __tablename__ = "rights_permission_history"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    rights_permission_id = Column(UUID(as_uuid=True), ForeignKey("rights_permissions.id", ondelete="CASCADE"), nullable=False)
    
    # What changed
    permission_type = Column(String(50), nullable=False)  # radio_play, public_display, podcast, commercial_use
    action = Column(String(20), nullable=False)  # granted, revoked, modified
    previous_value = Column(Boolean)
    new_value = Column(Boolean)
    
    # Who made the change
    changed_by_artist_id = Column(UUID(as_uuid=True), ForeignKey("artists.id"))
    changed_by_admin_id = Column(UUID(as_uuid=True), ForeignKey("admin_users.id"))
    
    # Additional context
    reason = Column(Text)
    notes = Column(Text)
    
    # When it happened
    changed_at = Column(DateTime(timezone=True), server_default=func.now())
    
    # Relationships
    rights_permission = relationship("RightsPermission")
    changed_by_artist = relationship("Artist", foreign_keys=[changed_by_artist_id])
    changed_by_admin = relationship("AdminUser", foreign_keys=[changed_by_admin_id])
