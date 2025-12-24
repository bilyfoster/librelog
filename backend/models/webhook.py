"""
Webhook model for external integrations (Discord, Slack, etc.)
"""

from sqlalchemy import Column, Integer, String, DateTime, Boolean, ForeignKey, Text, Enum as SQLEnum, text
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base
import enum


class WebhookType(str, enum.Enum):
    """Webhook type enumeration"""
    DISCORD = "DISCORD"
    SLACK = "SLACK"
    CUSTOM = "CUSTOM"


class WebhookEvent(str, enum.Enum):
    """Webhook event types"""
    LOG_PUBLISHED = "LOG_PUBLISHED"
    LOG_LOCKED = "LOG_LOCKED"
    ORDER_CREATED = "ORDER_CREATED"
    ORDER_APPROVED = "ORDER_APPROVED"
    INVOICE_CREATED = "INVOICE_CREATED"
    INVOICE_PAID = "INVOICE_PAID"
    CAMPAIGN_CREATED = "CAMPAIGN_CREATED"
    CAMPAIGN_UPDATED = "CAMPAIGN_UPDATED"
    COPY_EXPIRING = "COPY_EXPIRING"
    SPOT_CONFLICT = "SPOT_CONFLICT"
    USER_ACTIVITY = "USER_ACTIVITY"


class Webhook(Base):
    """Webhook model for external integrations"""
    __tablename__ = "webhooks"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    name = Column(String(255), nullable=False)
    webhook_type = Column(SQLEnum(WebhookType), nullable=False, index=True)
    url = Column(Text, nullable=False)
    events = Column(JSONB, nullable=False)  # Array of WebhookEvent enum values
    secret = Column(String(255), nullable=True)  # For HMAC signature verification
    active = Column(Boolean, default=True, nullable=False, index=True)
    last_triggered_at = Column(DateTime(timezone=True), nullable=True)
    last_error = Column(Text, nullable=True)
    error_count = Column(Integer, default=0)
    headers = Column(JSONB, nullable=True)  # Custom headers to include
    created_by = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    creator = relationship("User", foreign_keys=[created_by])

    def __repr__(self):
        return f"<Webhook(name='{self.name}', type='{self.webhook_type}')>"

