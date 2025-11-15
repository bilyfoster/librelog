"""
Copy Assignment model for linking copy to spots
"""

from sqlalchemy import Column, Integer, DateTime, ForeignKey
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class CopyAssignment(Base):
    """Copy Assignment model for linking copy/scripts to spots"""
    __tablename__ = "copy_assignments"

    id = Column(Integer, primary_key=True, index=True)
    spot_id = Column(Integer, ForeignKey("spots.id"), nullable=False, index=True)
    copy_id = Column(Integer, ForeignKey("copy.id"), nullable=False, index=True)
    order_id = Column(Integer, ForeignKey("orders.id"), nullable=True, index=True)
    assigned_at = Column(DateTime(timezone=True), server_default=func.now())
    assigned_by = Column(Integer, ForeignKey("users.id"), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    # Relationships
    spot = relationship("Spot", back_populates="copy_assignment")
    copy = relationship("Copy", back_populates="copy_assignments")
    order = relationship("Order", back_populates="copy_assignments")
    assigner = relationship("User", foreign_keys=[assigned_by])

    def __repr__(self):
        return f"<CopyAssignment(spot_id={self.spot_id}, copy_id={self.copy_id})>"

