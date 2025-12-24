"""
OrderRevision model for audit trail - WideOrbit-compatible
"""

from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Enum as SQLEnum, Text, text
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base
import enum


class RevisionReasonCode(str, enum.Enum):
    """Revision reason code enumeration"""
    PRICE_CHANGE = "PRICE_CHANGE"
    EXTENSION = "EXTENSION"
    CANCELLATION = "CANCELLATION"
    LINE_ADDED = "LINE_ADDED"
    LINE_REMOVED = "LINE_REMOVED"
    COPY_CHANGE = "COPY_CHANGE"
    DATE_CHANGE = "DATE_CHANGE"
    OTHER = "OTHER"


class OrderRevision(Base):
    """OrderRevision model for tracking order changes - WideOrbit-compatible"""
    __tablename__ = "order_revisions"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    order_id = Column(UUID(as_uuid=True), ForeignKey("orders.id"), nullable=False, index=True)
    
    # Revision Information
    revision_number = Column(Integer, nullable=False, index=True)
    changed_fields = Column(JSONB, nullable=True)  # JSON object with field names and old/new values
    reason_code = Column(SQLEnum(RevisionReasonCode), nullable=True, index=True)
    reason_notes = Column(Text, nullable=True)
    
    # Approval Status at Revision
    approval_status_at_revision = Column(String(50), nullable=True)
    
    # Change Tracking
    changed_by = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)
    changed_at = Column(DateTime(timezone=True), server_default=func.now(), index=True)
    
    # Timestamps
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    # Relationships
    order = relationship("Order", back_populates="revisions")
    changer = relationship("User", foreign_keys=[changed_by])

    def __repr__(self):
        return f"<OrderRevision(order_id={self.order_id}, revision_number={self.revision_number})>"

