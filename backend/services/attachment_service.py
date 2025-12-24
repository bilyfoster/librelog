"""
Attachment service for handling file uploads and downloads - WideOrbit-compatible
"""

import os
import uuid
from uuid import UUID
from pathlib import Path
from typing import Optional, BinaryIO
from fastapi import UploadFile, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from backend.models.order_attachment import OrderAttachment, AttachmentType
from backend.models.user import User


class AttachmentService:
    """Service for managing order attachments"""
    
    def __init__(self, db: AsyncSession, upload_base_path: str = "/uploads/orders"):
        self.db = db
        self.upload_base_path = Path(upload_base_path)
        self.upload_base_path.mkdir(parents=True, exist_ok=True)
    
    async def upload_file(
        self,
        file: UploadFile,
        attachment_type: AttachmentType,
        order_id: Optional[UUID] = None,
        order_line_id: Optional[UUID] = None,
        copy_id: Optional[UUID] = None,
        uploaded_by: User = None,
        description: Optional[str] = None,
        notes: Optional[str] = None
    ) -> OrderAttachment:
        """Upload a file and create attachment record"""
        
        if not uploaded_by:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="User must be authenticated to upload files"
            )
        
        # Validate at least one parent ID is provided
        if not any([order_id, order_line_id, copy_id]):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="At least one of order_id, order_line_id, or copy_id must be provided"
            )
        
        # Generate unique filename
        file_ext = Path(file.filename).suffix if file.filename else ""
        unique_filename = f"{uuid.uuid4()}{file_ext}"
        
        # Determine subdirectory based on attachment type
        type_dir = attachment_type.value.lower()
        upload_dir = self.upload_base_path / type_dir
        upload_dir.mkdir(parents=True, exist_ok=True)
        
        # If order_id provided, create order-specific subdirectory
        if order_id:
            upload_dir = upload_dir / str(order_id)
            upload_dir.mkdir(parents=True, exist_ok=True)
        
        file_path = upload_dir / unique_filename
        
        # Save file
        try:
            with open(file_path, "wb") as f:
                content = await file.read()
                f.write(content)
        except Exception as e:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"Failed to save file: {str(e)}"
            )
        
        # Get file size
        file_size = file_path.stat().st_size
        
        # Create attachment record
        attachment = OrderAttachment(
            order_id=order_id,
            order_line_id=order_line_id,
            copy_id=copy_id,
            file_path=str(file_path),
            file_name=file.filename or unique_filename,
            mime_type=file.content_type,
            file_size=file_size,
            attachment_type=attachment_type,
            description=description,
            notes=notes,
            uploaded_by=uploaded_by.id
        )
        
        self.db.add(attachment)
        await self.db.commit()
        await self.db.refresh(attachment)
        
        return attachment
    
    async def get_file_path(self, attachment_id: UUID) -> Path:
        """Get file path for an attachment"""
        from sqlalchemy import select
        
        result = await self.db.execute(
            select(OrderAttachment).where(OrderAttachment.id == attachment_id)
        )
        attachment = result.scalar_one_or_none()
        
        if not attachment:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Attachment not found"
            )
        
        file_path = Path(attachment.file_path)
        
        if not file_path.exists():
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="File not found on disk"
            )
        
        return file_path
    
    async def delete_attachment(self, attachment_id: UUID) -> bool:
        """Delete an attachment and its file"""
        from sqlalchemy import select
        
        result = await self.db.execute(
            select(OrderAttachment).where(OrderAttachment.id == attachment_id)
        )
        attachment = result.scalar_one_or_none()
        
        if not attachment:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Attachment not found"
            )
        
        # Delete file from filesystem
        file_path = Path(attachment.file_path)
        if file_path.exists():
            try:
                file_path.unlink()
            except Exception as e:
                # Log error but continue with DB deletion
                print(f"Warning: Failed to delete file {file_path}: {e}")
        
        # Delete database record
        from sqlalchemy import delete
        await self.db.execute(delete(OrderAttachment).where(OrderAttachment.id == attachment_id))
        await self.db.commit()
        
        return True
    
    async def list_attachments(
        self,
        order_id: Optional[UUID] = None,
        order_line_id: Optional[UUID] = None,
        copy_id: Optional[UUID] = None,
        attachment_type: Optional[AttachmentType] = None
    ) -> list[OrderAttachment]:
        """List attachments with optional filters"""
        from sqlalchemy import select
        
        query = select(OrderAttachment)
        
        if order_id:
            query = query.where(OrderAttachment.order_id == order_id)
        if order_line_id:
            query = query.where(OrderAttachment.order_line_id == order_line_id)
        if copy_id:
            query = query.where(OrderAttachment.copy_id == copy_id)
        if attachment_type:
            query = query.where(OrderAttachment.attachment_type == attachment_type)
        
        result = await self.db.execute(query)
        return list(result.scalars().all())

