"""
Copy router for audio asset and script management
"""

import os
from uuid import UUID
from pathlib import Path
from fastapi import APIRouter, Depends, HTTPException, status, Query, UploadFile, File, Form, Request
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from backend.database import get_db
from backend.models.copy import Copy
from backend.routers.auth import get_current_user
from backend.models.user import User
from backend.services.copy_service import CopyService
from backend.services.production_approval_service import ProductionApprovalService
from backend.models.copy import CopyStatus, CopyApprovalStatus
from backend.logging.audit import audit_logger
from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime, timezone
import aiofiles
import structlog

logger = structlog.get_logger()

# File storage directory for copy audio files
def _ensure_directory(path: str, fallback: str, env_var: str) -> str:
    """Ensure directory exists, with fallback if creation fails."""
    try:
        Path(path).mkdir(parents=True, exist_ok=True)
        return path
    except (PermissionError, FileNotFoundError, OSError) as e:
        # Fallback to /tmp if /var/lib is not writable or doesn't exist
        # Try fallback even if env var is set, to ensure the app can start
        logger.warning(f"Failed to create directory {path}: {e}, trying fallback {fallback}")
        try:
            Path(fallback).mkdir(parents=True, exist_ok=True)
            return fallback
        except Exception as e2:
            # Last resort: use /tmp directly
            logger.warning(f"Fallback also failed: {e2}, using {fallback} anyway")
            Path("/tmp").mkdir(parents=True, exist_ok=True)
            return fallback

COPY_FILES_DIR = _ensure_directory(
    os.getenv("COPY_FILES_DIR", "/var/lib/librelog/copy_files"),
    "/tmp/librelog/copy_files",
    "COPY_FILES_DIR"
)

router = APIRouter(redirect_slashes=False)  # Disable redirect to handle both /copy and /copy/


class CopyCreate(BaseModel):
    order_id: Optional[UUID] = None
    advertiser_id: Optional[UUID] = None
    title: str
    script_text: Optional[str] = None
    audio_file_path: Optional[str] = None
    audio_file_url: Optional[str] = None
    expires_at: Optional[datetime] = None


class CopyUpdate(BaseModel):
    title: Optional[str] = None
    script_text: Optional[str] = None
    audio_file_path: Optional[str] = None
    audio_file_url: Optional[str] = None
    expires_at: Optional[datetime] = None
    active: Optional[bool] = None


class CopyResponse(BaseModel):
    id: UUID
    order_id: Optional[UUID]
    advertiser_id: Optional[UUID]
    title: str
    script_text: Optional[str]
    audio_file_path: Optional[str]
    audio_file_url: Optional[str]
    version: int
    expires_at: Optional[str]
    active: bool
    created_at: str
    updated_at: str

    class Config:
        from_attributes = True


def copy_to_response_dict(copy_item: Copy) -> dict:
    """Convert Copy model to CopyResponse dict with proper datetime serialization"""
    return {
        "id": copy_item.id,
        "order_id": copy_item.order_id,
        "advertiser_id": copy_item.advertiser_id,
        "title": copy_item.title,
        "script_text": copy_item.script_text,
        "audio_file_path": copy_item.audio_file_path,
        "audio_file_url": copy_item.audio_file_url,
        "version": copy_item.version,
        "expires_at": copy_item.expires_at.isoformat() if copy_item.expires_at else None,
        "active": copy_item.active,
        "created_at": copy_item.created_at.isoformat() if copy_item.created_at else None,
        "updated_at": copy_item.updated_at.isoformat() if copy_item.updated_at else None
    }


@router.get("", response_model=list[CopyResponse])
@router.get("/", response_model=list[CopyResponse])
async def list_copy(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    order_id: Optional[UUID] = Query(None),
    advertiser_id: Optional[UUID] = Query(None),
    search: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all copy with optional filtering"""
    query = select(Copy)
    
    if order_id:
        query = query.where(Copy.order_id == order_id)
    
    if advertiser_id:
        query = query.where(Copy.advertiser_id == advertiser_id)
    
    if search:
        query = query.where(Copy.title.ilike(f"%{search}%"))
    
    query = query.offset(skip).limit(limit).order_by(Copy.title)
    
    result = await db.execute(query)
    copy_items = result.scalars().all()
    
    return [CopyResponse(**copy_to_response_dict(c)) for c in copy_items]


@router.post("", response_model=CopyResponse, status_code=status.HTTP_201_CREATED)
@router.post("/", response_model=CopyResponse, status_code=status.HTTP_201_CREATED)
async def create_copy(
    copy: CopyCreate,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new copy item"""
    try:
        copy_data = copy.model_dump()
        # Ensure active is set to True by default for new copy
        if 'active' not in copy_data:
            copy_data['active'] = True
        # Ensure enum values are set correctly
        # The EnumValueType TypeDecorator will handle converting enum instances to their values
        # But we need to ensure we're passing enum instances, not strings
        if 'copy_status' not in copy_data:
            copy_data['copy_status'] = CopyStatus.DRAFT
        elif isinstance(copy_data.get('copy_status'), str):
            # If it's a string, convert to enum instance
            try:
                copy_data['copy_status'] = CopyStatus(copy_data['copy_status'].lower())
            except ValueError:
                copy_data['copy_status'] = CopyStatus.DRAFT
        # If it's already a CopyStatus enum, leave it as is
        
        if 'copy_approval_status' not in copy_data:
            copy_data['copy_approval_status'] = CopyApprovalStatus.PENDING
        elif isinstance(copy_data.get('copy_approval_status'), str):
            # If it's a string, convert to enum instance
            try:
                copy_data['copy_approval_status'] = CopyApprovalStatus(copy_data['copy_approval_status'].lower())
            except ValueError:
                copy_data['copy_approval_status'] = CopyApprovalStatus.PENDING
        # If it's already a CopyApprovalStatus enum, leave it as is
        
        # Create the Copy object
        # The EnumValueType will convert enum instances to their values during process_bind_param
        new_copy = Copy(**copy_data)
        db.add(new_copy)
        await db.flush()  # Get ID without committing
        copy_id = new_copy.id
        
        try:
            await db.commit()
        except Exception as commit_error:
            # If commit fails due to relationship loading, refresh and continue
            if "relationship" in str(commit_error).lower() or "Multiple rows" in str(commit_error):
                await db.rollback()
                # Re-fetch the copy
                result = await db.execute(select(Copy).where(Copy.id == copy_id))
                new_copy = result.scalar_one_or_none()
                if not new_copy:
                    raise HTTPException(status_code=500, detail="Copy created but could not be retrieved")
            else:
                await db.rollback()
                raise HTTPException(status_code=500, detail=f"Failed to create copy: {str(commit_error)}")
        
        await db.refresh(new_copy)
    except HTTPException:
        raise
    except Exception as e:
        await db.rollback()
        raise HTTPException(status_code=500, detail=f"Failed to create copy: {str(e)}")
    await db.refresh(new_copy)
    
    # Log audit action
    client_ip = request.client.host if request.client else None
    user_agent = request.headers.get("user-agent")
    await audit_logger.log_action(
        db_session=db,
        user_id=current_user.id,
        action="CREATE_COPY",
        resource_type="Copy",
        resource_id=new_copy.id,
        details={
            "title": new_copy.title,
            "order_id": new_copy.order_id,
            "advertiser_id": new_copy.advertiser_id
        },
        ip_address=client_ip,
        user_agent=user_agent
    )
    
    return CopyResponse(**copy_to_response_dict(new_copy))


@router.post("/upload", response_model=CopyResponse, status_code=status.HTTP_201_CREATED)
async def upload_copy(
    file: UploadFile = File(...),
    title: str = Form(...),
    order_id: Optional[UUID] = Form(None),
    advertiser_id: Optional[UUID] = Form(None),
    script_text: Optional[str] = Form(None),
    expires_at: Optional[str] = Form(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Upload copy with audio file"""
    # Validate file type (audio files)
    allowed_extensions = {'.mp3', '.wav', '.m4a', '.ogg', '.flac', '.aac'}
    file_ext = Path(file.filename).suffix.lower() if file.filename else ''
    
    if file_ext not in allowed_extensions:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid file type. Allowed: {', '.join(allowed_extensions)}"
        )
    
    # Validate file size (max 50MB)
    file_size = 0
    content = await file.read()
    file_size = len(content)
    max_size = 50 * 1024 * 1024  # 50MB
    
    if file_size > max_size:
        raise HTTPException(
            status_code=400,
            detail=f"File too large. Maximum size is 50MB."
        )
    
    # Generate unique filename
    timestamp = datetime.now(timezone.utc).strftime("%Y%m%d_%H%M%S")
    safe_filename = f"{timestamp}_{file.filename}" if file.filename else f"{timestamp}_copy{file_ext}"
    file_path = Path(COPY_FILES_DIR) / safe_filename
    
    # Save file
    try:
        async with aiofiles.open(file_path, 'wb') as f:
            await f.write(content)
        
        # Generate file URL (relative path for serving)
        file_url = f"/api/copy/{safe_filename}/file"
        audio_file_path = str(file_path)
        
        # Parse expires_at if provided
        expires_datetime = None
        if expires_at:
            try:
                expires_datetime = datetime.fromisoformat(expires_at.replace('Z', '+00:00'))
            except ValueError:
                raise HTTPException(
                    status_code=400,
                    detail="Invalid expires_at format. Use ISO 8601 format."
                )
        
        # Create copy record
        new_copy = Copy(
            title=title,
            order_id=order_id,
            advertiser_id=advertiser_id,
            script_text=script_text,
            audio_file_path=audio_file_path,
            audio_file_url=file_url,
            expires_at=expires_datetime,
            active=True
        )
        
        db.add(new_copy)
        await db.commit()
        await db.refresh(new_copy)
        
        logger.info("Copy file uploaded", copy_id=new_copy.id, filename=safe_filename)
        
        return CopyResponse(**copy_to_response_dict(new_copy))
        
    except Exception as e:
        logger.error("Copy file upload failed", error=str(e), exc_info=True)
        # Clean up file if it was created
        if file_path.exists():
            file_path.unlink()
        raise HTTPException(
            status_code=500,
            detail=f"Failed to upload copy file: {str(e)}"
        )


@router.get("/expiring", response_model=list[CopyResponse])
async def get_expiring_copy(
    days_ahead: int = Query(30, ge=1, le=365),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get copy expiring soon"""
    copy_service = CopyService(db)
    expiring = await copy_service.check_expiring(days_ahead)
    return [CopyResponse(**copy_to_response_dict(c)) for c in expiring]


@router.get("/expiring", response_model=list[CopyResponse])
async def get_expiring_copy(
    days_ahead: int = Query(30, ge=1, le=365),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get copy expiring soon"""
    copy_service = CopyService(db)
    expiring = await copy_service.check_expiring(days_ahead)
    
    return [CopyResponse(**copy_to_response_dict(c)) for c in expiring]


@router.get("/{copy_id}", response_model=CopyResponse)
async def get_copy(
    copy_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific copy item"""
    result = await db.execute(select(Copy).where(Copy.id == copy_id))
    copy_item = result.scalar_one_or_none()
    
    if not copy_item:
        raise HTTPException(status_code=404, detail="Copy not found")
    
    return CopyResponse(**copy_to_response_dict(copy_item))


@router.put("/{copy_id}", response_model=CopyResponse)
async def update_copy(
    copy_id: UUID,
    copy_update: CopyUpdate,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update a copy item"""
    result = await db.execute(select(Copy).where(Copy.id == copy_id))
    copy_item = result.scalar_one_or_none()
    
    if not copy_item:
        raise HTTPException(status_code=404, detail="Copy not found")
    
    update_data = copy_update.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(copy_item, field, value)
    
    await db.commit()
    await db.refresh(copy_item)
    
    # Log audit action
    client_ip = request.client.host if request.client else None
    user_agent = request.headers.get("user-agent")
    await audit_logger.log_action(
        db_session=db,
        user_id=current_user.id,
        action="UPDATE_COPY",
        resource_type="Copy",
        resource_id=copy_id,
        details={
            "title": copy_item.title,
            "updated_fields": list(update_data.keys())
        },
        ip_address=client_ip,
        user_agent=user_agent
    )
    
    return CopyResponse(**copy_to_response_dict(copy_item))


@router.delete("/{copy_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_copy(
    copy_id: UUID,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete a copy item"""
    result = await db.execute(select(Copy).where(Copy.id == copy_id))
    copy_item = result.scalar_one_or_none()
    
    if not copy_item:
        raise HTTPException(status_code=404, detail="Copy not found")
    
    # Log audit action before deletion
    client_ip = request.client.host if request.client else None
    user_agent = request.headers.get("user-agent")
    await audit_logger.log_action(
        db_session=db,
        user_id=current_user.id,
        action="DELETE_COPY",
        resource_type="Copy",
        resource_id=copy_id,
        details={
            "title": copy_item.title,
            "order_id": copy_item.order_id
        },
        ip_address=client_ip,
        user_agent=user_agent
    )
    
    await db.delete(copy_item)
    await db.commit()
    
    return None


@router.get("/{filename}/file")
async def serve_copy_file(
    filename: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Serve copy audio file"""
    from fastapi.responses import FileResponse
    
    file_path = Path(COPY_FILES_DIR) / filename
    
    if not file_path.exists():
        raise HTTPException(status_code=404, detail="Copy file not found")
    
    # Verify the file belongs to a copy in the database
    result = await db.execute(
        select(Copy).where(Copy.audio_file_url.contains(filename))
    )
    copy_item = result.scalar_one_or_none()
    
    if not copy_item:
        raise HTTPException(status_code=404, detail="Copy not found")
    
    return FileResponse(
        path=str(file_path),
        media_type="audio/mpeg",
        filename=filename
    )


@router.post("/{copy_id}/assign")
async def assign_copy_to_spot(
    copy_id: UUID,
    spot_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Assign copy to a spot"""
    from backend.models.copy_assignment import CopyAssignment
    from backend.models.spot import Spot
    
    # Check if copy and spot exist
    copy_result = await db.execute(select(Copy).where(Copy.id == copy_id))
    copy_item = copy_result.scalar_one_or_none()
    if not copy_item:
        raise HTTPException(status_code=404, detail="Copy not found")
    
    spot_result = await db.execute(select(Spot).where(Spot.id == spot_id))
    spot = spot_result.scalar_one_or_none()
    if not spot:
        raise HTTPException(status_code=404, detail="Spot not found")
    
    # Check if assignment already exists
    existing = await db.execute(
        select(CopyAssignment).where(
            and_(CopyAssignment.spot_id == spot_id, CopyAssignment.copy_id == copy_id)
        )
    )
    if existing.scalar_one_or_none():
        raise HTTPException(status_code=400, detail="Copy already assigned to this spot")
    
    # Create assignment
    assignment = CopyAssignment(
        spot_id=spot_id,
        copy_id=copy_id,
        order_id=spot.order_id,
        assigned_by=current_user.id
    )
    db.add(assignment)
    await db.commit()
    await db.refresh(assignment)
    
    return {"message": "Copy assigned to spot successfully", "assignment_id": assignment.id}


@router.post("/{copy_id}/set-needs-production")
async def set_needs_production(
    copy_id: UUID,
    needs_production: bool = Query(...),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Set the needs_production flag on copy"""
    copy_service = CopyService(db)
    
    try:
        copy_item = await copy_service.set_needs_production(copy_id, needs_production)
        return CopyResponse(**copy_to_response_dict(copy_item))
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.post("/{copy_id}/approve")
async def approve_copy(
    copy_id: UUID,
    auto_create_po: bool = Query(True),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Approve copy and optionally auto-create production order if needs_production=true"""
    copy_service = CopyService(db)
    
    try:
        copy_item = await copy_service.approve_copy(copy_id, current_user.id, auto_create_po)
        return CopyResponse(**copy_to_response_dict(copy_item))
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/{copy_id}/reject")
async def reject_copy(
    copy_id: UUID,
    reason: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Reject copy"""
    approval_service = ProductionApprovalService(db)
    
    try:
        copy_item = await approval_service.reject_copy(copy_id, current_user.id, reason)
        return CopyResponse(**copy_to_response_dict(copy_item))
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.get("/{copy_id}/production-status")
async def get_production_status(
    copy_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get production status for a copy item"""
    copy_service = CopyService(db)
    
    status_info = await copy_service.get_production_status(copy_id)
    
    if not status_info:
        raise HTTPException(status_code=404, detail="Copy not found")
    
    return status_info

