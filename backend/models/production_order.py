"""
ProductionOrder model for production workflow management
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, Text, Date, ForeignKey, Enum as SQLEnum, Numeric
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base
import enum


class ProductionOrderType(str, enum.Enum):
    """Production order type enumeration"""
    NEW_SPOT = "new_spot"
    REVISION = "revision"
    RENEWAL = "renewal"
    TAG_ONLY = "tag_only"
    UPDATE = "update"
    RUSH_ORDER = "rush_order"


class ProductionOrderStatus(str, enum.Enum):
    """Production order status enumeration"""
    PENDING = "PENDING"
    ASSIGNED = "ASSIGNED"
    IN_PROGRESS = "IN_PROGRESS"
    QC = "QC"
    COMPLETED = "COMPLETED"
    DELIVERED = "DELIVERED"
    CANCELLED = "CANCELLED"


class ProductionOrder(Base):
    """ProductionOrder model for managing production workflow"""
    __tablename__ = "production_orders"

    id = Column(Integer, primary_key=True, index=True)
    po_number = Column(String(50), unique=True, nullable=False, index=True)  # PRO-YYYYMMDD-XXXX
    
    # One-to-one relationship with Copy
    copy_id = Column(Integer, ForeignKey("copy.id"), nullable=False, unique=True, index=True)
    
    # Links to Order, Campaign, Advertiser for context
    order_id = Column(Integer, ForeignKey("orders.id"), nullable=True, index=True)
    campaign_id = Column(Integer, ForeignKey("campaigns.id"), nullable=True, index=True)
    advertiser_id = Column(Integer, ForeignKey("advertisers.id"), nullable=False, index=True)
    
    # Basic information
    client_name = Column(String(255), nullable=False)
    campaign_title = Column(String(255), nullable=True)
    start_date = Column(Date, nullable=True)
    end_date = Column(Date, nullable=True)
    budget = Column(Numeric(10, 2), nullable=True)
    contract_number = Column(String(100), nullable=True)
    
    # Production requirements
    spot_lengths = Column(JSONB)  # Array of spot lengths in seconds, e.g., [30, 60]
    deliverables = Column(Text, nullable=True)  # Description of deliverables
    copy_requirements = Column(Text, nullable=True)  # Copy requirements description
    
    # Talent needs
    talent_needs = Column(JSONB, nullable=True)  # {type: "male", voice: "character", etc.}
    audio_references = Column(JSONB, nullable=True)  # Array of reference file paths/URLs
    
    # Instructions and metadata
    instructions = Column(Text, nullable=True)  # Instructions for producer
    deadline = Column(DateTime(timezone=True), nullable=True, index=True)
    stations = Column(JSONB, nullable=True)  # Array of station IDs or names
    version_count = Column(Integer, default=1)  # How many versions required
    
    # Order type and status
    order_type = Column(SQLEnum(ProductionOrderType), nullable=False, default=ProductionOrderType.NEW_SPOT)
    status = Column(SQLEnum(ProductionOrderStatus), nullable=False, default=ProductionOrderStatus.PENDING, index=True)
    
    # Timestamps
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    completed_at = Column(DateTime(timezone=True), nullable=True)
    delivered_at = Column(DateTime(timezone=True), nullable=True)
    
    # Relationships
    copy = relationship("Copy", back_populates="production_order", uselist=False, foreign_keys="Copy.production_order_id")
    order = relationship("Order", foreign_keys=[order_id])
    campaign = relationship("Campaign", foreign_keys=[campaign_id])
    advertiser = relationship("Advertiser", foreign_keys=[advertiser_id])
    assignments = relationship("ProductionAssignment", back_populates="production_order", cascade="all, delete-orphan")
    voice_talent_requests = relationship("VoiceTalentRequest", back_populates="production_order", cascade="all, delete-orphan")
    revisions = relationship("ProductionRevision", back_populates="production_order", cascade="all, delete-orphan")
    comments = relationship("ProductionComment", back_populates="production_order", cascade="all, delete-orphan")

    def __repr__(self):
        return f"<ProductionOrder(po_number='{self.po_number}', status='{self.status}')>"

