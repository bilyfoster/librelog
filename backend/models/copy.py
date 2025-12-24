"""
Copy model for audio asset and script management
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, Text, ForeignKey, Enum as SQLEnum, Date, Numeric, TypeDecorator, text
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from sqlalchemy.types import TypeDecorator as BaseTypeDecorator
from backend.database import Base
import enum


class CopyStatus(str, enum.Enum):
    """Copy status enumeration"""
    DRAFT = "draft"
    PENDING_APPROVAL = "pending_approval"
    APPROVED = "approved"
    IN_PRODUCTION = "in_production"
    COMPLETED = "completed"
    DELIVERED = "delivered"


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
        # CRITICAL: Convert enum instance to its value (lowercase string)
        # SQLAlchemy was using enum.name ("DRAFT") instead of enum.value ("draft")
        if isinstance(value, enum.Enum):
            return value.value  # Return "draft" not "DRAFT"
        # If it's already a string, ensure it's lowercase
        if isinstance(value, str):
            return value.lower()
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


class CopyApprovalStatus(str, enum.Enum):
    """Copy approval status enumeration"""
    PENDING = "pending"
    APPROVED = "approved"
    REJECTED = "rejected"


class CopyType(str, enum.Enum):
    """Copy type enumeration - WideOrbit compatible"""
    NEW = "NEW"
    TAG_UPDATE = "TAG_UPDATE"
    RENEWAL = "RENEWAL"
    SEASONAL = "SEASONAL"




class Copy(Base):
    """Copy model for managing audio assets and scripts"""
    __tablename__ = "copy"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    order_id = Column(UUID(as_uuid=True), ForeignKey("orders.id"), nullable=True, index=True)
    advertiser_id = Column(UUID(as_uuid=True), ForeignKey("advertisers.id"), nullable=True, index=True)
    title = Column(String(255), nullable=False, index=True)
    
    # WideOrbit Copy Fields
    copy_code = Column(String(100), nullable=True, index=True)  # ISCI Code
    isci_code = Column(String(100), nullable=True, index=True)  # Alternative ISCI field
    copy_type = Column(SQLEnum(CopyType), nullable=True, index=True)
    
    # Script and Audio
    script_text = Column(Text, nullable=True)
    audio_file_path = Column(Text, nullable=True)
    audio_file_url = Column(Text, nullable=True)
    client_provided_audio = Column(Text, nullable=True)  # Reference to client-provided audio file
    
    # Version and Dates
    version = Column(Integer, default=1)
    effective_date = Column(Date, nullable=True, index=True)
    expires_at = Column(DateTime(timezone=True), nullable=True, index=True)
    expiration_date = Column(Date, nullable=True, index=True)
    active = Column(Boolean, default=True, index=True)
    
    # Priority and Rotation
    priority = Column(Integer, nullable=True)  # Rotation weight
    rotation_mode = Column(String(50), nullable=True)  # SEQUENTIAL, RANDOM, WEIGHTED, DAYPART, PROGRAM
    
    # Multi-cut support
    cut_count = Column(Integer, default=0)  # Number of cuts for this copy
    
    # Political and live read flags
    political_flag = Column(Boolean, default=False, index=True)
    live_read_enabled = Column(Boolean, default=False, index=True)
    
    # Copy instructions
    copy_instructions = Column(Text, nullable=True)  # Instructions for talent
    instruction_attachments = Column(Text, nullable=True)  # JSON array of PDF/image paths
    production_notes = Column(Text, nullable=True)
    
    # Legal and Compliance
    legal_copy = Column(Text, nullable=True)  # Required disclaimers
    required_disclaimers = Column(Text, nullable=True)
    talent_restrictions = Column(Text, nullable=True)
    
    # AQH Reconciliation (for reporting)
    aqh_reconciliation = Column(JSONB, nullable=True)  # AQH reconciliation fields
    
    # Production workflow fields
    production_order_id = Column(UUID(as_uuid=True), ForeignKey("production_orders.id"), nullable=True, unique=True, index=True)
    # Use a custom TypeDecorator to ensure enum values (not names) are used for PostgreSQL native enums
    # SQLAlchemy with native_enum=True uses enum names ("DRAFT") instead of values ("draft")
    # This TypeDecorator converts enum instances to their value strings before database insertion
    copy_status = Column(EnumValueType(CopyStatus, native_enum=True, create_constraint=False, name='copystatus'), nullable=False, default=CopyStatus.DRAFT, index=True)
    needs_production = Column(Boolean, default=False, index=True)  # KEY FLAG: When true + copy approved → auto-create ProductionOrder
    copy_approval_status = Column(EnumValueType(CopyApprovalStatus, native_enum=True, create_constraint=False, name='copyapprovalstatus'), nullable=False, default=CopyApprovalStatus.PENDING, index=True)
    script_approved_at = Column(DateTime(timezone=True), nullable=True)
    script_approved_by = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=True)
    
    # Audio Metadata
    audio_format = Column(String(50), nullable=True)  # WAV, MP3, AAC
    audio_source = Column(String(100), nullable=True)
    loudness_check = Column(Boolean, default=False, index=True)
    
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    order = relationship("Order", back_populates="copy_items")
    advertiser = relationship("Advertiser", back_populates="copy_items")
    copy_assignments = relationship("CopyAssignment", back_populates="copy")
    audio_cuts = relationship("AudioCut", back_populates="copy", cascade="all, delete-orphan")
    production_order = relationship("ProductionOrder", back_populates="copy", uselist=False, foreign_keys=[production_order_id])
    script_approver = relationship("User", foreign_keys=[script_approved_by])
    attachments = relationship("OrderAttachment", back_populates="copy", cascade="all, delete-orphan")

    def __repr__(self):
        return f"<Copy(title='{self.title}', version={self.version})>"

