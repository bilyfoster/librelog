"""
ProductionAssignment model for assigning production work to users
"""

from sqlalchemy import Column, Integer, String, DateTime, Text, ForeignKey, Enum as SQLEnum
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base
import enum


class AssignmentType(str, enum.Enum):
    """Assignment type enumeration"""
    PRODUCER = "producer"
    VOICE_TALENT = "voice_talent"
    IMAGING = "imaging"
    QC = "qc"
    PRODUCTION_DIRECTOR = "production_director"


class AssignmentStatus(str, enum.Enum):
    """Assignment status enumeration"""
    PENDING = "PENDING"
    ACCEPTED = "ACCEPTED"
    IN_PROGRESS = "IN_PROGRESS"
    COMPLETED = "COMPLETED"
    REJECTED = "REJECTED"


class ProductionAssignment(Base):
    """ProductionAssignment model for assigning production work"""
    __tablename__ = "production_assignments"

    id = Column(Integer, primary_key=True, index=True)
    production_order_id = Column(Integer, ForeignKey("production_orders.id"), nullable=False, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    
    # Assignment details
    assignment_type = Column(SQLEnum(AssignmentType), nullable=False, index=True)
    status = Column(SQLEnum(AssignmentStatus), nullable=False, default=AssignmentStatus.PENDING, index=True)
    
    # Notes and metadata
    notes = Column(Text, nullable=True)
    
    # Timestamps
    assigned_at = Column(DateTime(timezone=True), server_default=func.now())
    accepted_at = Column(DateTime(timezone=True), nullable=True)
    completed_at = Column(DateTime(timezone=True), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    
    # Relationships
    production_order = relationship("ProductionOrder", back_populates="assignments")
    user = relationship("User", foreign_keys=[user_id])

    def __repr__(self):
        return f"<ProductionAssignment(production_order_id={self.production_order_id}, user_id={self.user_id}, type='{self.assignment_type}')>"

