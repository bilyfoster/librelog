"""
Inventory Slot model for inventory management
"""

from sqlalchemy import Column, Integer, String, DateTime, Date, Numeric, Boolean, CheckConstraint, text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
from backend.database import Base


class InventorySlot(Base):
    """Inventory Slot model for tracking available inventory"""
    __tablename__ = "inventory_slots"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    date = Column(Date, nullable=False, index=True)
    hour = Column(Integer, CheckConstraint("hour >= 0 AND hour <= 23"), nullable=False, index=True)
    break_position = Column(String(10))  # A, B, C, etc.
    daypart = Column(String(50))
    available = Column(Integer, default=3600)  # Available seconds in the hour
    booked = Column(Integer, default=0)  # Booked seconds
    sold_out = Column(Boolean, default=False, index=True)
    revenue = Column(Numeric(10, 2), default=0)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    def __repr__(self):
        return f"<InventorySlot(date='{self.date}', hour={self.hour}, available={self.available})>"

