"""
Campaign model for ad management
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, Date, Text
from sqlalchemy.sql import func
from backend.database import Base


class Campaign(Base):
    """Campaign model for ad management"""
    __tablename__ = "campaigns"

    id = Column(Integer, primary_key=True, index=True)
    advertiser = Column(String(255), nullable=False, index=True)
    start_date = Column(Date, nullable=False, index=True)
    end_date = Column(Date, nullable=False, index=True)
    priority = Column(Integer, default=1, index=True)
    file_url = Column(Text)
    active = Column(Boolean, default=True, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    def __repr__(self):
        return f"<Campaign(advertiser='{self.advertiser}', priority={self.priority})>"
