"""
Pydantic schemas for OrderAttachment models - WideOrbit-compatible
"""

from pydantic import BaseModel, ConfigDict
from typing import Optional
from datetime import datetime
from uuid import UUID
from backend.models.order_attachment import AttachmentType


class OrderAttachmentBase(BaseModel):
    """Base order attachment schema"""
    file_name: str
    mime_type: Optional[str] = None
    file_size: Optional[int] = None
    attachment_type: AttachmentType
    description: Optional[str] = None
    notes: Optional[str] = None


class OrderAttachmentCreate(OrderAttachmentBase):
    """Schema for creating a new order attachment"""
    order_id: Optional[UUID] = None
    order_line_id: Optional[UUID] = None
    copy_id: Optional[UUID] = None
    file_path: str


class OrderAttachmentUpdate(BaseModel):
    """Schema for updating an order attachment"""
    file_name: Optional[str] = None
    mime_type: Optional[str] = None
    attachment_type: Optional[AttachmentType] = None
    description: Optional[str] = None
    notes: Optional[str] = None


class OrderAttachmentResponse(OrderAttachmentBase):
    """Schema for order attachment response"""
    id: UUID
    order_id: Optional[UUID] = None
    order_line_id: Optional[UUID] = None
    copy_id: Optional[UUID] = None
    file_path: str
    uploaded_by: UUID
    uploaded_by_name: Optional[str] = None
    uploaded_at: datetime
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)

