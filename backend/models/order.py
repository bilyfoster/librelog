"""
Order model for traffic management - WideOrbit-compatible
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, Date, Numeric, ForeignKey, CheckConstraint, Enum as SQLEnum, Text, text
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base
import enum


class RateType(str, enum.Enum):
    """Rate type enumeration"""
    ROS = "ROS"  # Run of Schedule
    DAYPART = "DAYPART"  # Daypart-specific
    PROGRAM = "PROGRAM"  # Program-specific
    FIXED_TIME = "FIXED_TIME"  # Fixed time slot


class OrderStatus(str, enum.Enum):
    """Order status enumeration"""
    DRAFT = "DRAFT"
    PENDING = "PENDING"
    APPROVED = "APPROVED"
    ACTIVE = "ACTIVE"
    COMPLETED = "COMPLETED"
    CANCELLED = "CANCELLED"


class ApprovalStatus(str, enum.Enum):
    """Approval status enumeration"""
    PENDING = "PENDING"
    APPROVED = "APPROVED"
    REJECTED = "REJECTED"
    NOT_REQUIRED = "NOT_REQUIRED"


class OrderType(str, enum.Enum):
    """Order type enumeration - WideOrbit compatible"""
    LOCAL = "LOCAL"
    NATIONAL = "NATIONAL"
    NETWORK = "NETWORK"
    DIGITAL = "DIGITAL"
    NTR = "NTR"  # Non-traditional revenue


class BillingCycle(str, enum.Enum):
    """Billing cycle enumeration"""
    WEEKLY = "WEEKLY"
    MONTHLY = "MONTHLY"
    ONE_SHOT = "ONE_SHOT"
    BIWEEKLY = "BIWEEKLY"
    QUARTERLY = "QUARTERLY"


class InvoiceType(str, enum.Enum):
    """Invoice type enumeration"""
    STANDARD = "STANDARD"
    COOP = "COOP"
    POLITICAL = "POLITICAL"


class PoliticalClass(str, enum.Enum):
    """Political class enumeration"""
    FEDERAL = "FEDERAL"
    STATE = "STATE"
    LOCAL = "LOCAL"
    ISSUE = "ISSUE"


class Order(Base):
    """Order model for managing advertising orders/contracts - WideOrbit-compatible"""
    __tablename__ = "orders"

    # Basic Contract Info
    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    order_number = Column(String(50), unique=True, nullable=False, index=True)
    order_name = Column(String(255), nullable=True, index=True)  # Order title/name
    campaign_id = Column(UUID(as_uuid=True), ForeignKey("campaigns.id"), nullable=True, index=True)
    advertiser_id = Column(UUID(as_uuid=True), ForeignKey("advertisers.id"), nullable=False, index=True)
    agency_id = Column(UUID(as_uuid=True), ForeignKey("agencies.id"), nullable=True, index=True)
    sales_rep_id = Column(UUID(as_uuid=True), ForeignKey("sales_reps.id"), nullable=True, index=True)
    
    # Sales Team/Office/Region (keeping string fields for backward compatibility)
    sales_team = Column(String(100), nullable=True)
    sales_office = Column(String(100), nullable=True)
    sales_region = Column(String(100), nullable=True)
    
    # Optional foreign keys for quick reference
    sales_team_id = Column(UUID(as_uuid=True), ForeignKey("sales_teams.id"), nullable=True, index=True)
    sales_office_id = Column(UUID(as_uuid=True), ForeignKey("sales_offices.id"), nullable=True, index=True)
    sales_region_id = Column(UUID(as_uuid=True), ForeignKey("sales_regions.id"), nullable=True, index=True)
    
    # Station/Cluster
    stations = Column(JSONB, nullable=True)  # Array of station IDs or names
    cluster = Column(String(100), nullable=True)
    
    # Order Type
    order_type = Column(SQLEnum(OrderType), nullable=True, index=True)
    
    # Dates
    start_date = Column(Date, nullable=False, index=True)
    end_date = Column(Date, nullable=False, index=True)
    
    # Legacy spot fields (kept for backward compatibility)
    spot_lengths = Column(JSONB)  # Array of spot lengths in seconds, e.g., [30, 60]
    total_spots = Column(Integer, default=0)
    rate_type = Column(SQLEnum(RateType), nullable=False, default=RateType.ROS)
    rates = Column(JSONB)  # Rate structure based on rate_type
    
    # Financial Terms
    gross_amount = Column(Numeric(12, 2), default=0)
    net_amount = Column(Numeric(12, 2), default=0)
    total_value = Column(Numeric(12, 2), default=0)  # Total contract value
    agency_commission_percent = Column(Numeric(5, 2), nullable=True)  # Agency commission percentage
    agency_commission_amount = Column(Numeric(12, 2), nullable=True)  # Calculated agency commission
    agency_discount = Column(Numeric(12, 2), nullable=True)
    cash_discount = Column(Numeric(12, 2), nullable=True)
    trade_barter = Column(Boolean, default=False, index=True)
    trade_value = Column(Numeric(12, 2), nullable=True)
    taxable = Column(Boolean, default=False, index=True)
    billing_cycle = Column(SQLEnum(BillingCycle), nullable=True)
    invoice_type = Column(SQLEnum(InvoiceType), nullable=True)
    
    # Co-op fields
    coop_sponsor = Column(String(255), nullable=True)
    coop_percent = Column(Numeric(5, 2), nullable=True)  # Co-op % contribution
    
    # Client PO
    client_po_number = Column(String(100), nullable=True, index=True)
    
    # Billing Address/Contact
    billing_address = Column(Text, nullable=True)
    billing_contact = Column(String(255), nullable=True)
    billing_contact_email = Column(String(255), nullable=True)
    billing_contact_phone = Column(String(50), nullable=True)
    
    # Legal/Compliance
    political_class = Column(SQLEnum(PoliticalClass), nullable=True, index=True)
    political_window_flag = Column(Boolean, default=False, index=True)  # Lowest Unit Rate handling
    contract_reference = Column(String(100), nullable=True, index=True)  # Insertion Order Number
    insertion_order_number = Column(String(100), nullable=True, index=True)
    regulatory_notes = Column(Text, nullable=True)
    fcc_id = Column(String(100), nullable=True)
    required_disclaimers = Column(Text, nullable=True)
    
    # Status and Approval
    status = Column(SQLEnum(OrderStatus), nullable=False, default=OrderStatus.DRAFT, index=True)
    approval_status = Column(SQLEnum(ApprovalStatus), nullable=False, default=ApprovalStatus.NOT_REQUIRED)
    approved_by = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=True)
    approved_at = Column(DateTime(timezone=True), nullable=True)
    
    # Workflow Flags
    traffic_ready = Column(Boolean, default=False, index=True)
    billing_ready = Column(Boolean, default=False, index=True)
    locked = Column(Boolean, default=False, index=True)
    revision_number = Column(Integer, default=1)
    
    # Timestamps
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    created_by = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=True)

    # Relationships
    campaign = relationship("Campaign", back_populates="orders")
    advertiser = relationship("Advertiser", back_populates="orders")
    agency = relationship("Agency", back_populates="orders")
    sales_rep = relationship("SalesRep", back_populates="orders")
    sales_team_rel = relationship("SalesTeam", foreign_keys=[sales_team_id], back_populates="orders")
    sales_office_rel = relationship("SalesOffice", foreign_keys=[sales_office_id], back_populates="orders")
    sales_region_rel = relationship("SalesRegion", foreign_keys=[sales_region_id], back_populates="orders")
    spots = relationship("Spot", back_populates="order")
    copy_assignments = relationship("CopyAssignment", back_populates="order")
    copy_items = relationship("Copy", back_populates="order")
    invoices = relationship("Invoice", back_populates="order")
    approver = relationship("User", foreign_keys=[approved_by])
    creator = relationship("User", foreign_keys=[created_by])
    order_lines = relationship("OrderLine", back_populates="order", cascade="all, delete-orphan")
    attachments = relationship("OrderAttachment", back_populates="order", cascade="all, delete-orphan")
    revisions = relationship("OrderRevision", back_populates="order", cascade="all, delete-orphan")
    workflow_states = relationship("OrderWorkflowState", back_populates="order", cascade="all, delete-orphan")

    def __repr__(self):
        return f"<Order(order_number='{self.order_number}', status='{self.status}')>"

