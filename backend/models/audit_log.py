"""
Audit Log model for tracking user actions
"""

from sqlalchemy import Column, Integer, String, DateTime, ForeignKey
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class AuditLog(Base):
    """Audit Log model for tracking all user actions"""
    __tablename__ = "audit_logs"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    action = Column(String(100), nullable=False, index=True)  # e.g., "CREATE_ORDER", "UPDATE_SPOT"
    resource_type = Column(String(50), nullable=False, index=True)  # e.g., "Order", "Spot", "Invoice"
    resource_id = Column(Integer, nullable=True, index=True)
    details = Column(String)  # Text field for change details (legacy: was changes JSONB)
    ip_address = Column(String(45))  # IPv6 compatible
    user_agent = Column(String(500))
    created_at = Column(DateTime(timezone=True), server_default=func.now(), index=True)

    # Relationships
    user = relationship("User", back_populates="audit_logs")

    def __repr__(self):
        return f"<AuditLog(user_id={self.user_id}, action='{self.action}', resource_type='{self.resource_type}')>"

