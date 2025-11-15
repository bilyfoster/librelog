"""
Audio Cuts router for managing multi-cut audio assets
"""

import os
from pathlib import Path
from fastapi import APIRouter, Depends, HTTPException, status, Query, UploadFile, File, Form
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from backend.database import get_db
from backend.models.audio_cut import AudioCut
from backend.routers.auth import get_current_user
from backend.models.user import User
from backend.services.audio_cut_service import AudioCutService
from backend.services.audio_qc_service import AudioQCService
from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime, timezone
import aiofiles
import structlog

logger = structlog.get_logger()

# File storage directory for audio cuts
AUDIO_CUTS_DIR = os.getenv("AUDIO_CUTS_DIR", "/var/lib/librelog/audio_cuts")
try:
    Path(AUDIO_CUTS_DIR).mkdir(parents=True, exist_ok=True)
except PermissionError:
    AUDIO_CUTS_DIR = os.getenv("AUDIO_CUTS_DIR", "/tmp/librelog/audio_cuts")
    Path(AUDIO_CUTS_DIR).mkdir(parents=True, exist_ok=True)

router = APIRouter(prefix="/audio-cuts", tags=["audio-cuts"])


class AudioCutCreate(BaseModel):
    copy_id: int
    cut_id: str
    cut_name: Optional[str] = None
    rotation_weight: float = 1.0
    daypart_restrictions: Optional[List[int]] = None
    program_associations: Optional[List[int]] = None
    expires_at: Optional[datetime] = None
    notes: Optional[str] = None
    tags: Optional[List[str]] = None


class AudioCutUpdate(BaseModel):
    cut_name: Optional[str] = None
    rotation_weight: Optional[float] = None
    daypart_restrictions: Optional[List[int]] = None
    program_associations: Optional[List[int]] = None
    expires_at: Optional[datetime] = None
    active: Optional[bool] = None
    notes: Optional[str] = None
    tags: Optional[List[str]] = None


class AudioCutResponse(BaseModel):
    id: int
    copy_id: int
    cut_id: str
    cut_name: Optional[str]
    audio_file_path: Optional[str]
    audio_file_url: Optional[str]
    file_checksum: Optional[str]
    version: int
    rotation_weight: float
    daypart_restrictions: Optional[List[int]]
    program_associations: Optional[List[int]]
    expires_at: Optional[str]
    active: bool
    qc_results: Optional[dict]
    notes: Optional[str]
    tags: Optional[List[str]]
    created_at: str
    updated_at: str

    class Config:
        from_attributes = True


@router.get("/", response_model=List[AudioCutResponse])
async def list_cuts(
    copy_id: Optional[int] = Query(None),
    active_only: bool = Query(False),
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List audio cuts"""
    cut_service = AudioCutService(db)
    
    if copy_id:
        cuts = await cut_service.get_cuts_by_copy(copy_id, active_only=active_only)
        return [AudioCutResponse.model_validate(c) for c in cuts[skip:skip+limit]]
    else:
        query = select(AudioCut)
        if active_only:
            query = query.where(AudioCut.active == True)
        query = query.offset(skip).limit(limit).order_by(AudioCut.created_at.desc())
        result = await db.execute(query)
        cuts = result.scalars().all()
        return [AudioCutResponse.model_validate(c) for c in cuts]


@router.get("/{cut_id}", response_model=AudioCutResponse)
async def get_cut(
    cut_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific cut"""
    cut_service = AudioCutService(db)
    cut = await cut_service.get_cut(cut_id)
    if not cut:
        raise HTTPException(status_code=404, detail="Cut not found")
    return AudioCutResponse.model_validate(cut)


@router.post("/", response_model=AudioCutResponse, status_code=status.HTTP_201_CREATED)
async def create_cut(
    cut_data: AudioCutCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new audio cut"""
    cut_service = AudioCutService(db)
    try:
        cut = await cut_service.create_cut(
            copy_id=cut_data.copy_id,
            cut_id=cut_data.cut_id,
            cut_name=cut_data.cut_name,
            rotation_weight=cut_data.rotation_weight,
            daypart_restrictions=cut_data.daypart_restrictions,
            program_associations=cut_data.program_associations,
            expires_at=cut_data.expires_at,
            notes=cut_data.notes,
            tags=cut_data.tags,
            created_by=current_user.id
        )
        return AudioCutResponse.model_validate(cut)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/upload", response_model=AudioCutResponse, status_code=status.HTTP_201_CREATED)
async def upload_cut_audio(
    file: UploadFile = File(...),
    copy_id: int = Form(...),
    cut_id: str = Form(...),
    cut_name: Optional[str] = Form(None),
    rotation_weight: float = Form(1.0),
    expires_at: Optional[str] = Form(None),
    notes: Optional[str] = Form(None),
    tags: Optional[str] = Form(None),  # JSON string
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Upload audio file for a cut"""
    # Validate file type
    allowed_extensions = {'.mp3', '.wav', '.m4a', '.ogg', '.flac', '.aac'}
    file_ext = Path(file.filename).suffix.lower() if file.filename else ''
    
    if file_ext not in allowed_extensions:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid file type. Allowed: {', '.join(allowed_extensions)}"
        )
    
    # Generate unique filename
    timestamp = datetime.now(timezone.utc).strftime("%Y%m%d_%H%M%S")
    safe_filename = f"{timestamp}_{cut_id}_{file.filename}" if file.filename else f"{timestamp}_{cut_id}_cut{file_ext}"
    file_path = Path(AUDIO_CUTS_DIR) / safe_filename
    
    # Save file
    try:
        async with aiofiles.open(file_path, 'wb') as f:
            content = await file.read()
            await f.write(content)
        
        # Parse tags
        tag_list = None
        if tags:
            import json
            try:
                tag_list = json.loads(tags)
            except:
                tag_list = [tags]
        
        # Parse expires_at
        expires_datetime = None
        if expires_at:
            try:
                expires_datetime = datetime.fromisoformat(expires_at.replace('Z', '+00:00'))
            except ValueError:
                raise HTTPException(
                    status_code=400,
                    detail="Invalid expires_at format. Use ISO 8601 format."
                )
        
        # Create cut
        cut_service = AudioCutService(db)
        cut = await cut_service.create_cut(
            copy_id=copy_id,
            cut_id=cut_id,
            cut_name=cut_name,
            audio_file_path=str(file_path),
            audio_file_url=f"/api/audio-cuts/{safe_filename}/file",
            rotation_weight=rotation_weight,
            expires_at=expires_datetime,
            notes=notes,
            tags=tag_list,
            created_by=current_user.id
        )
        
        # Run QC on uploaded file
        qc_service = AudioQCService(db)
        try:
            await qc_service.run_qc(str(file_path), cut_id=cut.id)
        except Exception as e:
            logger.warning("QC failed for uploaded file", cut_id=cut.id, error=str(e))
        
        logger.info("Audio cut uploaded", cut_id=cut.id, filename=safe_filename)
        return AudioCutResponse.model_validate(cut)
        
    except Exception as e:
        logger.error("Cut upload failed", error=str(e), exc_info=True)
        if file_path.exists():
            file_path.unlink()
        raise HTTPException(
            status_code=500,
            detail=f"Failed to upload cut: {str(e)}"
        )


@router.put("/{cut_id}", response_model=AudioCutResponse)
async def update_cut(
    cut_id: int,
    cut_data: AudioCutUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update a cut"""
    cut_service = AudioCutService(db)
    try:
        cut = await cut_service.update_cut(
            cut_id=cut_id,
            cut_name=cut_data.cut_name,
            rotation_weight=cut_data.rotation_weight,
            daypart_restrictions=cut_data.daypart_restrictions,
            program_associations=cut_data.program_associations,
            expires_at=cut_data.expires_at,
            active=cut_data.active,
            notes=cut_data.notes,
            tags=cut_data.tags
        )
        return AudioCutResponse.model_validate(cut)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.delete("/{cut_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_cut(
    cut_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete a cut"""
    cut_service = AudioCutService(db)
    try:
        await cut_service.delete_cut(cut_id)
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.post("/{cut_id}/versions", response_model=dict)
async def create_version(
    cut_id: int,
    version_notes: Optional[str] = Form(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new version of a cut"""
    cut_service = AudioCutService(db)
    try:
        version = await cut_service.create_version(
            cut_id=cut_id,
            version_notes=version_notes,
            changed_by=current_user.id
        )
        return {"message": "Version created", "version_id": version.id}
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/{cut_id}/versions", response_model=List[dict])
async def get_versions(
    cut_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get all versions for a cut"""
    cut_service = AudioCutService(db)
    versions = await cut_service.get_versions(cut_id)
    return [
        {
            "id": v.id,
            "version_number": v.version_number,
            "audio_file_path": v.audio_file_path,
            "file_checksum": v.file_checksum,
            "version_notes": v.version_notes,
            "created_at": v.created_at.isoformat() if v.created_at else None
        }
        for v in versions
    ]


@router.post("/{cut_id}/rollback/{version_number}", response_model=AudioCutResponse)
async def rollback_to_version(
    cut_id: int,
    version_number: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Rollback a cut to a previous version"""
    cut_service = AudioCutService(db)
    try:
        cut = await cut_service.rollback_to_version(cut_id, version_number)
        return AudioCutResponse.model_validate(cut)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/{cut_id}/file")
async def serve_cut_file(
    cut_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Serve the audio file for a cut"""
    from fastapi.responses import FileResponse
    
    cut_service = AudioCutService(db)
    cut = await cut_service.get_cut(cut_id)
    if not cut:
        raise HTTPException(status_code=404, detail="Cut not found")
    
    if not cut.audio_file_path or not os.path.exists(cut.audio_file_path):
        raise HTTPException(status_code=404, detail="Audio file not found")
    
    return FileResponse(
        path=cut.audio_file_path,
        media_type="audio/mpeg",
        filename=f"{cut.cut_id}_{cut.version}.{Path(cut.audio_file_path).suffix[1:]}"
    )

