"""
OrderAttachment model for file attachments - WideOrbit-compatible
"""

from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Enum as SQLEnum, Text, BigInteger, text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from backend.database import Base
import enum


class AttachmentType(str, enum.Enum):
    """Attachment type enumeration"""
    CONTRACT = "CONTRACT"
    AUDIO = "AUDIO"
    SCRIPT = "SCRIPT"
    CREATIVE = "CREATIVE"
    LEGAL = "LEGAL"
    IO = "IO"  # Insertion Order
    EMAIL = "EMAIL"
    OTHER = "OTHER"


class OrderAttachment(Base):
    """OrderAttachment model for file attachment metadata - WideOrbit-compatible"""
    __tablename__ = "order_attachments"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"), index=True)
    
    # Foreign keys (nullable - attachment can be tied to order, line, or copy)
    order_id = Column(UUID(as_uuid=True), ForeignKey("orders.id"), nullable=True, index=True)
    order_line_id = Column(UUID(as_uuid=True), ForeignKey("order_lines.id"), nullable=True, index=True)
    copy_id = Column(UUID(as_uuid=True), ForeignKey("copy.id"), nullable=True, index=True)
    
    # File Information
    file_path = Column(Text, nullable=False)  # Filesystem path or S3 URL
    file_name = Column(String(255), nullable=False)
    mime_type = Column(String(100), nullable=True)
    file_size = Column(BigInteger, nullable=True)  # Size in bytes
    
    # Attachment Metadata
    attachment_type = Column(SQLEnum(AttachmentType), nullable=False, index=True)
    description = Column(Text, nullable=True)
    notes = Column(Text, nullable=True)
    
    # Upload Information
    uploaded_by = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)
    uploaded_at = Column(DateTime(timezone=True), server_default=func.now(), index=True)
    
    # Timestamps
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    # Relationships
    order = relationship("Order", back_populates="attachments")
    order_line = relationship("OrderLine", back_populates="attachments")
    copy = relationship("Copy", back_populates="attachments")
    uploader = relationship("User", foreign_keys=[uploaded_by])

    def __repr__(self):
        return f"<OrderAttachment(file_name='{self.file_name}', type='{self.attachment_type}')>"

