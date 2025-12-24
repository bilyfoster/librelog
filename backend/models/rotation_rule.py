"""
Rotation Rule model for traffic scheduling
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, ForeignKey, Text, CheckConstraint, text
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base
import enum


class RotationType(str, enum.Enum):
    """Rotation type enumeration"""
    SEQUENTIAL = "SEQUENTIAL"  # Play in order
    RANDOM = "RANDOM"  # Random order
    WEIGHTED = "WEIGHTED"  # Weighted random
    EVEN = "EVEN"  # Even distribution


class RotationRule(Base):
    """Rotation Rule model for managing spot rotation"""
    __tablename__ = "rotation_rules"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    name = Column(String(100), nullable=False, index=True)
    description = Column(Text, nullable=True)
    
    # Rotation configuration
    rotation_type = Column(String(20), nullable=False, default=RotationType.SEQUENTIAL.value)
    daypart_id = Column(UUID(as_uuid=True), ForeignKey("dayparts.id"), nullable=True, index=True)
    campaign_id = Column(UUID(as_uuid=True), ForeignKey("campaigns.id"), nullable=True, index=True)
    
    # Rotation parameters
    min_separation = Column(Integer, default=0)  # Minimum spots between same campaign
    max_per_hour = Column(Integer, nullable=True)  # Maximum spots per hour
    max_per_day = Column(Integer, nullable=True)  # Maximum spots per day
    weights = Column(JSONB, nullable=True)  # Weighted rotation weights {campaign_id: weight}
    
    # Cut-specific rotation support
    cut_specific = Column(Boolean, default=False)  # Whether this rule applies to specific cuts
    cut_weights = Column(JSONB, nullable=True)  # Weighted rotation weights {cut_id: weight}
    program_specific = Column(Boolean, default=False)  # Whether this rule is program-specific
    
    # Scheduling rules
    exclude_days = Column(JSONB, nullable=True)  # Days to exclude (0=Monday, 6=Sunday)
    exclude_times = Column(JSONB, nullable=True)  # Time ranges to exclude
    priority = Column(Integer, default=0)  # Rule priority (higher = more important)
    
    # Status
    active = Column(Boolean, default=True, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    
    # Relationships
    daypart = relationship("Daypart", foreign_keys=[daypart_id])
    campaign = relationship("Campaign", foreign_keys=[campaign_id])

    def __repr__(self):
        return f"<RotationRule(name='{self.name}', type='{self.rotation_type}')>"

