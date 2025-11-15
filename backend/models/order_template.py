"""
Order Template model for quick order creation
"""

from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Text
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class OrderTemplate(Base):
    """Order Template model for quick order entry"""
    __tablename__ = "order_templates"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(255), nullable=False, index=True)
    description = Column(Text)
    default_spot_lengths = Column(JSONB)  # Array of default spot lengths
    default_rate_type = Column(String(20))  # ROS, DAYPART, PROGRAM, FIXED_TIME
    default_rates = Column(JSONB)  # Default rate structure
    created_by = Column(Integer, ForeignKey("users.id"), nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    creator = relationship("User", foreign_keys=[created_by])

    def __repr__(self):
        return f"<OrderTemplate(name='{self.name}')>"

