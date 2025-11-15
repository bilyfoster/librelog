"""
Makegood model for traffic management
"""

from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Boolean
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class Makegood(Base):
    """Makegood model for tracking makegood spots"""
    __tablename__ = "makegoods"

    id = Column(Integer, primary_key=True, index=True)
    original_spot_id = Column(Integer, ForeignKey("spots.id"), nullable=False, index=True)
    makegood_spot_id = Column(Integer, ForeignKey("spots.id"), nullable=False, index=True)
    campaign_id = Column(Integer, ForeignKey("campaigns.id"), nullable=True, index=True)
    reason = Column(String(500))
    approved_by = Column(Integer, ForeignKey("users.id"), nullable=True)
    approved_at = Column(DateTime(timezone=True), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    # Relationships
    original_spot = relationship("Spot", foreign_keys=[original_spot_id], backref="original_makegoods")
    makegood_spot = relationship("Spot", foreign_keys=[makegood_spot_id], backref="makegood_spots")
    campaign = relationship("Campaign", back_populates="makegoods")
    approver = relationship("User", foreign_keys=[approved_by])

    def __repr__(self):
        return f"<Makegood(original_spot_id={self.original_spot_id}, makegood_spot_id={self.makegood_spot_id})>"

