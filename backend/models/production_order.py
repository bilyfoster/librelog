"""
ProductionOrder model for production workflow management
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, Text, Date, ForeignKey, Enum as SQLEnum, Numeric, text
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from sqlalchemy.types import TypeDecorator as BaseTypeDecorator
from backend.database import Base
import enum


class ProductionOrderType(str, enum.Enum):
    """Production order type enumeration"""
    # Values must match database enum exactly (uppercase)
    NEW_SPOT = "NEW_SPOT"
    REVISION = "REVISION"
    RENEWAL = "RENEWAL"
    TAG_ONLY = "TAG_ONLY"
    UPDATE = "UPDATE"
    RUSH_ORDER = "RUSH_ORDER"
    SPEC = "SPEC"  # Speculative spot (no order yet) - Note: may need to be added to DB enum


class EnumValueType(BaseTypeDecorator):
    """Type decorator to ensure enum values (not names) are used for PostgreSQL native enums"""
    impl = String
    cache_ok = True
    
    def __init__(self, enum_class, *args, **kwargs):
        self.enum_class = enum_class
        # Store the enum name for casting (needed for PostgreSQL native enums)
        self._enum_name = kwargs.get('name', enum_class.__name__.lower())
        # Remove enum-specific kwargs since we're using String as impl
        kwargs.pop('native_enum', None)
        kwargs.pop('create_constraint', None)
        kwargs.pop('name', None)
        # Call super with String as the base type
        super().__init__(String(50), **kwargs)
    
    def bind_expression(self, bindvalue):
        """Cast the bind value to the enum type for PostgreSQL"""
        from sqlalchemy import cast
        from sqlalchemy.dialects.postgresql import ENUM
        # Create an ENUM type with the same name (don't create the type, it already exists)
        enum_type = ENUM(name=self._enum_name, create_type=False)
        # Cast the bind value to the enum type
        return cast(bindvalue, enum_type)
    
    def process_bind_param(self, value, dialect):
        """Convert enum instance to its value string before binding to database"""
        if value is None:
            return None
        # CRITICAL: Convert enum instance to its value string
        # SQLAlchemy was using enum.name ("SPEC") instead of enum.value ("SPEC" or "spec")
        if isinstance(value, enum.Enum):
            return value.value  # Return the enum value
        # If it's already a string, return as-is
        if isinstance(value, str):
            return value
        return value
    
    def process_result_value(self, value, dialect):
        """Convert value string back to enum instance when reading from database"""
        if value is None:
            return None
        if isinstance(value, str):
            try:
                return self.enum_class(value)
            except ValueError:
                # If value doesn't match, try to find by value
                for enum_member in self.enum_class:
                    if enum_member.value == value:
                        return enum_member
                return None
        return value


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

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    po_number = Column(String(50), unique=True, nullable=False, index=True)  # PRO-YYYYMMDD-XXXX
    
    # One-to-one relationship with Copy
    copy_id = Column(UUID(as_uuid=True), ForeignKey("copy.id"), nullable=False, unique=True, index=True)
    
    # Links to Order, Campaign, Advertiser for context
    order_id = Column(UUID(as_uuid=True), ForeignKey("orders.id"), nullable=True, index=True)
    campaign_id = Column(UUID(as_uuid=True), ForeignKey("campaigns.id"), nullable=True, index=True)
    advertiser_id = Column(UUID(as_uuid=True), ForeignKey("advertisers.id"), nullable=True, index=True)  # Nullable for spec spots
    
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
    # Use EnumValueType to ensure enum values (not names) are used for PostgreSQL native enums
    order_type = Column(EnumValueType(ProductionOrderType, native_enum=True, create_constraint=False, name='productionordertype'), nullable=False, default=ProductionOrderType.NEW_SPOT)
    status = Column(EnumValueType(ProductionOrderStatus, native_enum=True, create_constraint=False, name='productionorderstatus'), nullable=False, default=ProductionOrderStatus.PENDING, index=True)
    
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

