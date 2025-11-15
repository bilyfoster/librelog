"""
Settings model for storing application configuration
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, Text, Index
from sqlalchemy.sql import func
from backend.database import Base


class Setting(Base):
    """Settings model for storing key-value configuration pairs"""
    __tablename__ = "settings"

    id = Column(Integer, primary_key=True, index=True)
    category = Column(String(50), nullable=False, index=True)  # e.g., 'smtp', 'storage', 'general', 'backup'
    key = Column(String(100), nullable=False, index=True)
    value = Column(Text, nullable=True)  # Encrypted for sensitive values
    encrypted = Column(Boolean, default=False, nullable=False)
    description = Column(Text, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    __table_args__ = (
        Index('ix_settings_category_key', 'category', 'key', unique=True),
    )

    def __repr__(self):
        return f"<Setting(category='{self.category}', key='{self.key}')>"

