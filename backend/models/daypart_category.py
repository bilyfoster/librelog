"""
Daypart Category model for organizing dayparts
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, Text, text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
from backend.database import Base


class DaypartCategory(Base):
    """Daypart Category model for categorizing dayparts"""
    __tablename__ = "daypart_categories"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    name = Column(String(100), nullable=False, unique=True, index=True)
    description = Column(Text, nullable=True)
    color = Column(String(7), nullable=True)  # Hex color code for UI
    icon = Column(String(50), nullable=True)  # Icon name for UI
    sort_order = Column(Integer, default=0, index=True)
    active = Column(Boolean, default=True, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    def __repr__(self):
        return f"<DaypartCategory(name='{self.name}')>"

