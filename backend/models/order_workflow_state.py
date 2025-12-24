"""
OrderWorkflowState model for workflow state machine tracking - WideOrbit-compatible
"""

from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Enum as SQLEnum, Text, text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base
import enum


class WorkflowState(str, enum.Enum):
    """Workflow state enumeration"""
    DRAFT = "DRAFT"
    PENDING_APPROVAL = "PENDING_APPROVAL"
    APPROVED = "APPROVED"
    TRAFFIC_READY = "TRAFFIC_READY"
    BILLING_READY = "BILLING_READY"
    LOCKED = "LOCKED"
    CANCELLED = "CANCELLED"


class OrderWorkflowState(Base):
    """OrderWorkflowState model for tracking workflow state transitions - WideOrbit-compatible"""
    __tablename__ = "order_workflow_states"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    order_id = Column(UUID(as_uuid=True), ForeignKey("orders.id"), nullable=False, index=True)
    
    # State Information
    state = Column(SQLEnum(WorkflowState), nullable=False, index=True)
    notes = Column(Text, nullable=True)
    
    # State Change Tracking
    state_changed_by = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)
    state_changed_at = Column(DateTime(timezone=True), server_default=func.now(), index=True)
    
    # Timestamps
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    # Relationships
    order = relationship("Order", back_populates="workflow_states")
    changer = relationship("User", foreign_keys=[state_changed_by])

    def __repr__(self):
        return f"<OrderWorkflowState(order_id={self.order_id}, state='{self.state}')>"

