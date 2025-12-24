"""
LiveRead model for live read script management and performance tracking
"""

from sqlalchemy import Column, String, DateTime, Boolean, Text, ForeignKey, text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class LiveRead(Base):
    """LiveRead model for managing live read scripts and performance tracking"""
    __tablename__ = "live_reads"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    
    # Association with copy/order
    copy_id = Column(UUID(as_uuid=True), ForeignKey("copy.id"), nullable=True, index=True)
    order_id = Column(UUID(as_uuid=True), ForeignKey("orders.id"), nullable=True, index=True)
    advertiser_id = Column(UUID(as_uuid=True), ForeignKey("advertisers.id"), nullable=True, index=True)
    
    # Script information
    script_text = Column(Text, nullable=False)
    script_title = Column(String(255), nullable=True)
    
    # Scheduling
    scheduled_time = Column(DateTime(timezone=True), nullable=True, index=True)
    scheduled_date = Column(DateTime(timezone=True), nullable=True, index=True)
    
    # Performance tracking
    performed_time = Column(DateTime(timezone=True), nullable=True, index=True)
    performed_by = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=True)  # Talent/DJ who performed it
    confirmed = Column(Boolean, default=False, index=True)
    
    # Proof of performance
    proof_of_performance = Column(Text, nullable=True)  # JSON or text record
    confirmation_timestamp = Column(DateTime(timezone=True), nullable=True)
    
    # Status
    status = Column(String(50), default="scheduled", index=True)  # scheduled, performed, missed, makegood_required
    makegood_required = Column(Boolean, default=False, index=True)
    makegood_id = Column(UUID(as_uuid=True), ForeignKey("makegoods.id"), nullable=True)
    
    # Metadata
    notes = Column(Text, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    
    # Relationships
    copy = relationship("Copy", foreign_keys=[copy_id])
    order = relationship("Order", foreign_keys=[order_id])
    advertiser = relationship("Advertiser", foreign_keys=[advertiser_id])
    performer = relationship("User", foreign_keys=[performed_by])
    makegood = relationship("Makegood", foreign_keys=[makegood_id])
    
    def __repr__(self):
        return f"<LiveRead(id={self.id}, status='{self.status}', scheduled={self.scheduled_time})>"

