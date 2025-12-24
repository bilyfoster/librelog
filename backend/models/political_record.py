"""
PoliticalRecord model for FCC compliance tracking and political ad management
"""

from sqlalchemy import Column, String, DateTime, Boolean, Text, ForeignKey, text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class PoliticalRecord(Base):
    """PoliticalRecord model for tracking political advertisements and FCC compliance"""
    __tablename__ = "political_records"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    
    # Association with copy/order
    copy_id = Column(UUID(as_uuid=True), ForeignKey("copy.id"), nullable=True, index=True)
    order_id = Column(UUID(as_uuid=True), ForeignKey("orders.id"), nullable=True, index=True)
    advertiser_id = Column(UUID(as_uuid=True), ForeignKey("advertisers.id"), nullable=True, index=True)
    
    # Political advertiser information
    advertiser_category = Column(String(100), nullable=False)  # political, issue_advocacy, etc.
    sponsor_name = Column(String(255), nullable=False, index=True)
    sponsor_id = Column(String(100), nullable=True)  # FEC ID or similar
    office_sought = Column(String(255), nullable=True)  # e.g., "U.S. Senate", "Governor"
    
    # Disclaimers
    disclaimers_required = Column(Text, nullable=True)  # Required disclaimer text
    disclaimers_included = Column(Boolean, default=False)
    
    # Compliance tracking
    compliance_status = Column(String(50), default="pending", index=True)  # pending, compliant, non_compliant
    no_substitution_allowed = Column(Boolean, default=True)  # Political ads cannot be substituted
    
    # Archive management (FCC requires 2-year retention)
    archived = Column(Boolean, default=False, index=True)
    archive_date = Column(DateTime(timezone=True), nullable=True)
    archive_location = Column(Text, nullable=True)  # Where archived files are stored
    
    # Political window rules
    political_window_start = Column(DateTime(timezone=True), nullable=True)
    political_window_end = Column(DateTime(timezone=True), nullable=True)
    no_preemptions = Column(Boolean, default=True)  # Cannot be preempted
    priority_scheduling = Column(Boolean, default=True)  # Priority in scheduling
    
    # WideOrbit Political Fields
    election_cycle = Column(String(100), nullable=True, index=True)
    lowest_unit_rate_record = Column(Text, nullable=True)  # LUR record
    class_comparison = Column(Text, nullable=True)  # Class comparison data
    window_dates = Column(Text, nullable=True)  # Political window dates
    substantiation_documentation = Column(Text, nullable=True)  # References to substantiation docs
    
    # Metadata
    notes = Column(Text, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    
    # Relationships
    copy = relationship("Copy", foreign_keys=[copy_id])
    order = relationship("Order", foreign_keys=[order_id])
    advertiser = relationship("Advertiser", foreign_keys=[advertiser_id])
    
    def __repr__(self):
        return f"<PoliticalRecord(sponsor='{self.sponsor_name}', status='{self.compliance_status}')>"

