"""
ProductionRevision model for tracking revision requests and history
"""

from sqlalchemy import Column, Integer, String, DateTime, Text, ForeignKey, text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class ProductionRevision(Base):
    """ProductionRevision model for tracking production revisions"""
    __tablename__ = "production_revisions"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    production_order_id = Column(UUID(as_uuid=True), ForeignKey("production_orders.id"), nullable=False, index=True)
    
    # Revision details
    revision_number = Column(Integer, nullable=False)  # 1, 2, 3, etc.
    requested_by = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)
    reason = Column(Text, nullable=False)  # Why the revision was requested
    
    # Revision notes
    notes = Column(Text, nullable=True)
    
    # Version branching - link to previous version
    previous_revision_id = Column(UUID(as_uuid=True), ForeignKey("production_revisions.id"), nullable=True)
    
    # Timestamps
    requested_at = Column(DateTime(timezone=True), server_default=func.now())
    completed_at = Column(DateTime(timezone=True), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    # Relationships
    production_order = relationship("ProductionOrder", back_populates="revisions")
    requester = relationship("User", foreign_keys=[requested_by])
    previous_revision = relationship("ProductionRevision", remote_side=[id], backref="next_revisions")

    def __repr__(self):
        return f"<ProductionRevision(production_order_id={self.production_order_id}, revision_number={self.revision_number})>"

