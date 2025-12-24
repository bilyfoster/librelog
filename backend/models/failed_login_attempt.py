"""
Failed login attempt model for tracking account lockout
"""

from sqlalchemy import Column, String, DateTime, Index, text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
from backend.database import Base


class FailedLoginAttempt(Base):
    """Model for tracking failed login attempts"""
    __tablename__ = "failed_login_attempts"
    
    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    username = Column(String(50), nullable=False, index=True)
    ip_address = Column(String(45), nullable=True)  # IPv6 can be up to 45 chars
    user_agent = Column(String(255), nullable=True)
    attempted_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    
    # Index for efficient queries by username and time
    __table_args__ = (
        Index('idx_username_attempted_at', 'username', 'attempted_at'),
    )
    
    def __repr__(self):
        return f"<FailedLoginAttempt(username='{self.username}', attempted_at='{self.attempted_at}')>"


