"""
OrderLine model for line-level order items - WideOrbit-compatible
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, Date, Numeric, ForeignKey, Enum as SQLEnum, Text, text
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base
import enum


class RevenueType(str, enum.Enum):
    """Revenue type enumeration"""
    SPOT = "SPOT"
    DIGITAL = "DIGITAL"
    TRADE = "TRADE"
    PROMOTION = "PROMOTION"
    NTR = "NTR"


class SelloutClass(str, enum.Enum):
    """Sellout class enumeration"""
    ROS = "ROS"  # Run of Schedule
    FIXED_POSITION = "FIXED_POSITION"
    BONUS = "BONUS"
    GUARANTEED = "GUARANTEED"


class OrderLine(Base):
    """OrderLine model for line-level order items - WideOrbit-compatible"""
    __tablename__ = "order_lines"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    order_id = Column(UUID(as_uuid=True), ForeignKey("orders.id"), nullable=False, index=True)
    
    # Line Item Basics
    line_number = Column(Integer, nullable=False)  # Sequential line number within order
    start_date = Column(Date, nullable=False, index=True)
    end_date = Column(Date, nullable=False, index=True)
    station_id = Column(UUID(as_uuid=True), ForeignKey("stations.id"), nullable=False, index=True)
    station = Column(String(100), nullable=True, index=True)  # Legacy: Station name (deprecated, use station_id)
    product = Column(String(255), nullable=True)  # e.g., Morning Drive, Stream, Podcast insert
    revenue_type = Column(SQLEnum(RevenueType), nullable=True, index=True)
    
    # Spot Details
    length = Column(Integer, nullable=True)  # Length in seconds (:10, :15, :30, :60)
    daypart = Column(String(50), nullable=True, index=True)  # Daypart name or ID
    days_of_week = Column(String(7), nullable=True)  # MTWTFSS format, e.g., "MTWTFSS" or "MTW"
    
    # Rate Information
    rate = Column(Numeric(12, 2), nullable=True)
    rate_type = Column(String(50), nullable=True)  # per spot, package, fixed
    sellout_class = Column(SQLEnum(SelloutClass), nullable=True)
    priority_code = Column(String(50), nullable=True, index=True)
    
    # Spot Frequency
    spot_frequency = Column(Integer, nullable=True)  # Number of spots
    estimated_impressions = Column(Integer, nullable=True)  # For digital/hybrid
    
    # Calculated Fields
    cpm = Column(Numeric(10, 2), nullable=True)  # Cost per thousand impressions
    cpp = Column(Numeric(10, 2), nullable=True)  # Cost per point
    
    # Scheduling Flags
    makegood_eligible = Column(Boolean, default=True, index=True)
    guaranteed_position = Column(Boolean, default=False, index=True)
    preemptible = Column(Boolean, default=True, index=True)
    
    # Inventory and Product
    inventory_class = Column(String(100), nullable=True)
    product_code = Column(String(100), nullable=True, index=True)
    
    # Deal Points and Notes
    deal_points = Column(Text, nullable=True)
    notes = Column(Text, nullable=True)
    
    # Digital/Streaming Add-Ons
    platform = Column(String(100), nullable=True)  # stream, app, podcast, web preroll
    impressions_booked = Column(Integer, nullable=True)
    delivery_window = Column(String(100), nullable=True)
    targeting_parameters = Column(JSONB, nullable=True)  # Geo, device, demographic targeting
    companion_banners = Column(JSONB, nullable=True)  # Banner sizes, URLs
    
    # Timestamps
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    order = relationship("Order", back_populates="order_lines")
    station = relationship("Station", back_populates="order_lines")
    attachments = relationship("OrderAttachment", back_populates="order_line", cascade="all, delete-orphan")

    def __repr__(self):
        return f"<OrderLine(order_id={self.order_id}, line_number={self.line_number})>"

