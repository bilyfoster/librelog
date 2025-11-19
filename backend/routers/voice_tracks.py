"""
Voice tracks router
"""

import os
from datetime import datetime
from pathlib import Path
from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File, Form, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func
from typing import Optional
from pydantic import BaseModel
from backend.database import get_db
from backend.models.voice_track import VoiceTrack
from backend.routers.auth import get_current_user
from backend.models.user import User
import aiofiles
import structlog

logger = structlog.get_logger()

router = APIRouter()

# File storage directory for voice tracks
def _ensure_directory(path: str, fallback: str) -> str:
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

VOICE_TRACKS_DIR = _ensure_directory(
    os.getenv("VOICE_TRACKS_DIR", "/var/lib/librelog/voice_tracks"),
    "/tmp/librelog/voice_tracks"
)


class VoiceTrackResponse(BaseModel):
    id: int
    show_name: Optional[str]
    file_url: str
    scheduled_time: Optional[datetime]
    uploaded_by: int
    created_at: datetime

    class Config:
        from_attributes = True


@router.get("/", response_model=list[VoiceTrackResponse])
async def list_voice_tracks(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    show_name: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all voice tracks"""
    query = select(VoiceTrack)
    
    if show_name:
        query = query.where(VoiceTrack.show_name.ilike(f"%{show_name}%"))
    
    # Get total count
    count_query = select(func.count(VoiceTrack.id))
    if show_name:
        count_query = count_query.where(VoiceTrack.show_name.ilike(f"%{show_name}%"))
    total_result = await db.execute(count_query)
    total = total_result.scalar() or 0
    
    # Get paginated results
    query = query.order_by(VoiceTrack.created_at.desc()).offset(skip).limit(limit)
    result = await db.execute(query)
    voice_tracks = result.scalars().all()
    
    return [VoiceTrackResponse.model_validate(vt) for vt in voice_tracks]


@router.post("/upload", response_model=VoiceTrackResponse, status_code=status.HTTP_201_CREATED)
async def upload_voice_track(
    file: UploadFile = File(...),
    show_name: Optional[str] = Form(None),
    scheduled_time: Optional[str] = Form(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Upload a voice track"""
    # Validate file type (audio files)
    allowed_extensions = {'.mp3', '.wav', '.m4a', '.ogg', '.flac', '.aac'}
    file_ext = Path(file.filename).suffix.lower() if file.filename else ''
    
    if file_ext not in allowed_extensions:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid file type. Allowed: {', '.join(allowed_extensions)}"
        )
    
    # Generate unique filename
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    safe_filename = f"{timestamp}_{file.filename}" if file.filename else f"{timestamp}_voice_track{file_ext}"
    file_path = Path(VOICE_TRACKS_DIR) / safe_filename
    
    # Save file
    try:
        async with aiofiles.open(file_path, 'wb') as f:
            content = await file.read()
            await f.write(content)
        
        # Generate file URL (relative path for serving)
        file_url = f"/api/voice/{safe_filename}/file"
        
        # Parse scheduled_time if provided
        scheduled_datetime = None
        if scheduled_time:
            try:
                scheduled_datetime = datetime.fromisoformat(scheduled_time.replace('Z', '+00:00'))
            except ValueError:
                raise HTTPException(
                    status_code=400,
                    detail="Invalid scheduled_time format. Use ISO 8601 format."
                )
        
        # Create voice track record
        voice_track = VoiceTrack(
            show_name=show_name,
            file_url=file_url,
            scheduled_time=scheduled_datetime,
            uploaded_by=current_user.id
        )
        
        db.add(voice_track)
        await db.commit()
        await db.refresh(voice_track)
        
        logger.info("Voice track uploaded", voice_track_id=voice_track.id, filename=safe_filename)
        
        return VoiceTrackResponse.model_validate(voice_track)
        
    except Exception as e:
        logger.error("Voice track upload failed", error=str(e), exc_info=True)
        # Clean up file if it was created
        if file_path.exists():
            file_path.unlink()
        raise HTTPException(
            status_code=500,
            detail=f"Failed to upload voice track: {str(e)}"
        )


@router.get("/{voice_track_id}", response_model=VoiceTrackResponse)
async def get_voice_track(
    voice_track_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific voice track"""
    result = await db.execute(select(VoiceTrack).where(VoiceTrack.id == voice_track_id))
    voice_track = result.scalar_one_or_none()
    
    if not voice_track:
        raise HTTPException(status_code=404, detail="Voice track not found")
    
    return VoiceTrackResponse.model_validate(voice_track)


@router.delete("/{voice_track_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_voice_track(
    voice_track_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete a voice track"""
    result = await db.execute(select(VoiceTrack).where(VoiceTrack.id == voice_track_id))
    voice_track = result.scalar_one_or_none()
    
    if not voice_track:
        raise HTTPException(status_code=404, detail="Voice track not found")
    
    # Extract filename from file_url and delete file
    if voice_track.file_url:
        try:
            # file_url format: /api/voice/{filename}/file
            filename = voice_track.file_url.split('/')[-2] if '/' in voice_track.file_url else None
            if filename:
                file_path = Path(VOICE_TRACKS_DIR) / filename
                if file_path.exists():
                    file_path.unlink()
                    logger.info("Voice track file deleted", filename=filename)
        except Exception as e:
            logger.warning("Failed to delete voice track file", error=str(e))
    
    # Delete database record
    await db.delete(voice_track)
    await db.commit()
    
    return None


@router.get("/{filename}/file")
async def serve_voice_track_file(
    filename: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Serve voice track file"""
    from fastapi.responses import FileResponse
    
    file_path = Path(VOICE_TRACKS_DIR) / filename
    
    if not file_path.exists():
        raise HTTPException(status_code=404, detail="Voice track file not found")
    
    # Verify the file belongs to a voice track in the database
    # Extract filename from the stored file_url pattern
    result = await db.execute(
        select(VoiceTrack).where(VoiceTrack.file_url.contains(filename))
    )
    voice_track = result.scalar_one_or_none()
    
    if not voice_track:
        raise HTTPException(status_code=404, detail="Voice track not found")
    
    return FileResponse(
        path=str(file_path),
        media_type="audio/mpeg",
        filename=filename
    )
