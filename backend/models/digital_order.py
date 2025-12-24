"""
Digital Order model for multi-platform orders (Future Phase)
"""

from sqlalchemy import Column, Integer, String, DateTime, Numeric, ForeignKey, Enum as SQLEnum, text
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base
import enum


class Platform(str, enum.Enum):
    """Platform enumeration"""
    WEBSITE = "WEBSITE"
    PODCAST = "PODCAST"
    STREAMING = "STREAMING"
    SOCIAL_MEDIA = "SOCIAL_MEDIA"


class DigitalOrderStatus(str, enum.Enum):
    """Digital order status enumeration"""
    DRAFT = "DRAFT"
    ACTIVE = "ACTIVE"
    COMPLETED = "COMPLETED"
    CANCELLED = "CANCELLED"


class DigitalOrder(Base):
    """Digital Order model for multi-platform campaign support"""
    __tablename__ = "digital_orders"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    order_id = Column(UUID(as_uuid=True), ForeignKey("orders.id"), nullable=False, index=True)
    platform = Column(SQLEnum(Platform), nullable=False)
    impression_target = Column(Integer, nullable=True)
    cpm = Column(Numeric(10, 2), nullable=True)
    cpc = Column(Numeric(10, 2), nullable=True)
    flat_rate = Column(Numeric(10, 2), nullable=True)
    creative_assets = Column(JSONB)  # Array of asset URLs/paths
    delivered_impressions = Column(Integer, default=0)
    status = Column(SQLEnum(DigitalOrderStatus), nullable=False, default=DigitalOrderStatus.DRAFT, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    order = relationship("Order", backref="digital_orders")

    def __repr__(self):
        return f"<DigitalOrder(order_id={self.order_id}, platform='{self.platform}')>"

