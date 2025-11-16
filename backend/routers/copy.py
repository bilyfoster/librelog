"""
Copy router for audio asset and script management
"""

import os
from pathlib import Path
from fastapi import APIRouter, Depends, HTTPException, status, Query, UploadFile, File, Form
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from backend.database import get_db
from backend.models.copy import Copy
from backend.routers.auth import get_current_user
from backend.models.user import User
from backend.services.copy_service import CopyService
from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime, timezone
import aiofiles
import structlog

logger = structlog.get_logger()

# File storage directory for copy audio files
COPY_FILES_DIR = os.getenv("COPY_FILES_DIR", "/var/lib/librelog/copy_files")
try:
    Path(COPY_FILES_DIR).mkdir(parents=True, exist_ok=True)
except PermissionError:
    # Fallback to /tmp if /var/lib is not writable
    COPY_FILES_DIR = os.getenv("COPY_FILES_DIR", "/tmp/librelog/copy_files")
    Path(COPY_FILES_DIR).mkdir(parents=True, exist_ok=True)

router = APIRouter()


class CopyCreate(BaseModel):
    order_id: Optional[int] = None
    advertiser_id: Optional[int] = None
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
    id: int
    order_id: Optional[int]
    advertiser_id: Optional[int]
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


@router.get("/", response_model=list[CopyResponse])
async def list_copy(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    order_id: Optional[int] = Query(None),
    advertiser_id: Optional[int] = Query(None),
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


@router.post("/", response_model=CopyResponse, status_code=status.HTTP_201_CREATED)
async def create_copy(
    copy: CopyCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new copy item"""
    new_copy = Copy(**copy.model_dump())
    db.add(new_copy)
    await db.commit()
    await db.refresh(new_copy)
    
    return CopyResponse(**copy_to_response_dict(new_copy))


@router.post("/upload", response_model=CopyResponse, status_code=status.HTTP_201_CREATED)
async def upload_copy(
    file: UploadFile = File(...),
    title: str = Form(...),
    order_id: Optional[int] = Form(None),
    advertiser_id: Optional[int] = Form(None),
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


@router.get("/{copy_id}", response_model=CopyResponse)
async def get_copy(
    copy_id: int,
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
    copy_id: int,
    copy_update: CopyUpdate,
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
    
    return CopyResponse(**copy_to_response_dict(copy_item))


@router.delete("/{copy_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_copy(
    copy_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete a copy item"""
    result = await db.execute(select(Copy).where(Copy.id == copy_id))
    copy_item = result.scalar_one_or_none()
    
    if not copy_item:
        raise HTTPException(status_code=404, detail="Copy not found")
    
    await db.delete(copy_item)
    await db.commit()
    
    return None


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
    copy_id: int,
    spot_id: int,
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

