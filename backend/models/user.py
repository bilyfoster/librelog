"""
User model and authentication
"""

from sqlalchemy import Column, String, DateTime, CheckConstraint, text
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base


class User(Base):
    """User model for authentication and authorization"""
    __tablename__ = "users"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    username = Column(String(50), unique=True, nullable=False, index=True)
    password_hash = Column(String(255), nullable=False)
    role = Column(
        String(20),
        CheckConstraint("role IN ('admin', 'producer', 'dj', 'sales', 'production_director', 'voice_talent')"),
        nullable=False,
        default="dj"
    )
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    last_login = Column(DateTime(timezone=True), nullable=True)
    last_activity = Column(DateTime(timezone=True), nullable=True)
    permissions = Column(JSONB, nullable=True)  # Array of permission strings

    # Relationships
    generated_logs = relationship("DailyLog", foreign_keys="DailyLog.generated_by", back_populates="generator")
    uploaded_voice_tracks = relationship("VoiceTrack", back_populates="uploader")
    audit_logs = relationship("AuditLog", back_populates="user")
    sales_rep = relationship("SalesRep", back_populates="user", uselist=False)
    stations = relationship("Station", secondary="user_stations", back_populates="users")

    def __repr__(self):
        return f"<User(username='{self.username}', role='{self.role}')>"
