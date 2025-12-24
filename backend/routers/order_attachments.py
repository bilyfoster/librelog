"""
Order Attachments router for file attachment management - WideOrbit-compatible
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query, UploadFile, File, Form
from uuid import UUID
from fastapi.responses import FileResponse
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from backend.database import get_db
from backend.models.order_attachment import OrderAttachment, AttachmentType
from backend.routers.auth import get_current_user
from backend.models.user import User
from backend.schemas.order_attachment import OrderAttachmentCreate, OrderAttachmentUpdate, OrderAttachmentResponse
from backend.services.attachment_service import AttachmentService
from typing import List, Optional

router = APIRouter()


@router.get("/", response_model=List[OrderAttachmentResponse])
async def list_attachments(
    order_id: Optional[UUID] = Query(None),
    order_line_id: Optional[UUID] = Query(None),
    copy_id: Optional[UUID] = Query(None),
    attachment_type: Optional[AttachmentType] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List attachments with optional filters"""
    attachment_service = AttachmentService(db)
    attachments = await attachment_service.list_attachments(
        order_id=order_id,
        order_line_id=order_line_id,
        copy_id=copy_id,
        attachment_type=attachment_type
    )
    
    # Load uploader names
    result = []
    for attachment in attachments:
        attachment_dict = {
            "id": attachment.id,
            "order_id": attachment.order_id,
            "order_line_id": attachment.order_line_id,
            "copy_id": attachment.copy_id,
            "file_path": attachment.file_path,
            "file_name": attachment.file_name,
            "mime_type": attachment.mime_type,
            "file_size": attachment.file_size,
            "attachment_type": attachment.attachment_type,
            "description": attachment.description,
            "notes": attachment.notes,
            "uploaded_by": attachment.uploaded_by,
            "uploaded_by_name": attachment.uploader.username if attachment.uploader else None,
            "uploaded_at": attachment.uploaded_at,
            "created_at": attachment.created_at,
            "updated_at": attachment.updated_at,
        }
        result.append(OrderAttachmentResponse(**attachment_dict))
    
    return result


@router.post("/", response_model=OrderAttachmentResponse, status_code=status.HTTP_201_CREATED)
async def upload_attachment(
    file: UploadFile = File(...),
    attachment_type: AttachmentType = Form(...),
    order_id: Optional[UUID] = Form(None),
    order_line_id: Optional[UUID] = Form(None),
    copy_id: Optional[UUID] = Form(None),
    description: Optional[str] = Form(None),
    notes: Optional[str] = Form(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Upload a new attachment"""
    attachment_service = AttachmentService(db)
    
    attachment = await attachment_service.upload_file(
        file=file,
        attachment_type=attachment_type,
        order_id=order_id,
        order_line_id=order_line_id,
        copy_id=copy_id,
        uploaded_by=current_user,
        description=description,
        notes=notes
    )
    
    # Load uploader name
    await db.refresh(attachment, ["uploader"])
    
    attachment_dict = {
        "id": attachment.id,
        "order_id": attachment.order_id,
        "order_line_id": attachment.order_line_id,
        "copy_id": attachment.copy_id,
        "file_path": attachment.file_path,
        "file_name": attachment.file_name,
        "mime_type": attachment.mime_type,
        "file_size": attachment.file_size,
        "attachment_type": attachment.attachment_type,
        "description": attachment.description,
        "notes": attachment.notes,
        "uploaded_by": attachment.uploaded_by,
        "uploaded_by_name": attachment.uploader.username if attachment.uploader else None,
        "uploaded_at": attachment.uploaded_at,
        "created_at": attachment.created_at,
        "updated_at": attachment.updated_at,
    }
    
    return OrderAttachmentResponse(**attachment_dict)


@router.get("/{attachment_id}", response_model=OrderAttachmentResponse)
async def get_attachment(
    attachment_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific attachment"""
    result = await db.execute(
        select(OrderAttachment).where(OrderAttachment.id == attachment_id)
    )
    attachment = result.scalar_one_or_none()
    
    if not attachment:
        raise HTTPException(status_code=404, detail="Attachment not found")
    
    # Load uploader name
    await db.refresh(attachment, ["uploader"])
    
    attachment_dict = {
        "id": attachment.id,
        "order_id": attachment.order_id,
        "order_line_id": attachment.order_line_id,
        "copy_id": attachment.copy_id,
        "file_path": attachment.file_path,
        "file_name": attachment.file_name,
        "mime_type": attachment.mime_type,
        "file_size": attachment.file_size,
        "attachment_type": attachment.attachment_type,
        "description": attachment.description,
        "notes": attachment.notes,
        "uploaded_by": attachment.uploaded_by,
        "uploaded_by_name": attachment.uploader.username if attachment.uploader else None,
        "uploaded_at": attachment.uploaded_at,
        "created_at": attachment.created_at,
        "updated_at": attachment.updated_at,
    }
    
    return OrderAttachmentResponse(**attachment_dict)


@router.get("/{attachment_id}/download")
async def download_attachment(
    attachment_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Download an attachment file"""
    attachment_service = AttachmentService(db)
    file_path = await attachment_service.get_file_path(attachment_id)
    
    # Get attachment for filename
    result = await db.execute(
        select(OrderAttachment).where(OrderAttachment.id == attachment_id)
    )
    attachment = result.scalar_one_or_none()
    
    if not attachment:
        raise HTTPException(status_code=404, detail="Attachment not found")
    
    return FileResponse(
        path=str(file_path),
        filename=attachment.file_name,
        media_type=attachment.mime_type or "application/octet-stream"
    )


@router.put("/{attachment_id}", response_model=OrderAttachmentResponse)
async def update_attachment(
    attachment_id: UUID,
    attachment_update: OrderAttachmentUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update attachment metadata"""
    result = await db.execute(
        select(OrderAttachment).where(OrderAttachment.id == attachment_id)
    )
    attachment = result.scalar_one_or_none()
    
    if not attachment:
        raise HTTPException(status_code=404, detail="Attachment not found")
    
    # Update fields
    update_data = attachment_update.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(attachment, field, value)
    
    await db.commit()
    await db.refresh(attachment, ["uploader"])
    
    attachment_dict = {
        "id": attachment.id,
        "order_id": attachment.order_id,
        "order_line_id": attachment.order_line_id,
        "copy_id": attachment.copy_id,
        "file_path": attachment.file_path,
        "file_name": attachment.file_name,
        "mime_type": attachment.mime_type,
        "file_size": attachment.file_size,
        "attachment_type": attachment.attachment_type,
        "description": attachment.description,
        "notes": attachment.notes,
        "uploaded_by": attachment.uploaded_by,
        "uploaded_by_name": attachment.uploader.username if attachment.uploader else None,
        "uploaded_at": attachment.uploaded_at,
        "created_at": attachment.created_at,
        "updated_at": attachment.updated_at,
    }
    
    return OrderAttachmentResponse(**attachment_dict)


@router.delete("/{attachment_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_attachment(
    attachment_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete an attachment and its file"""
    attachment_service = AttachmentService(db)
    await attachment_service.delete_attachment(attachment_id)
    return None

