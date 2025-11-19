"""
Voice tracks router
"""

import os
from datetime import datetime
from pathlib import Path
from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File, Form, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func, delete
from typing import Optional
from pydantic import BaseModel
from backend.database import get_db
from backend.models.voice_track import VoiceTrack, VoiceTrackStatus, BreakType
from backend.models.voice_track_slot import VoiceTrackSlot
from backend.models.voice_track_audit import VoiceTrackAudit
from backend.models.track import Track
from backend.services.audio_preview_service import AudioPreviewService
from backend.services.audio_processing_service import AudioProcessingService
from backend.services.timing_service import TimingService
from backend.services.voice_track_slot_service import VoiceTrackSlotService
from backend.routers.auth import get_current_user
from backend.models.user import User
from backend.integrations.libretime_client import libretime_client
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
    break_id: Optional[int] = None
    playlist_id: Optional[int] = None
    break_type: Optional[str] = None
    slot_position: Optional[str] = None
    ramp_time: Optional[float] = None
    back_time: Optional[float] = None
    take_number: int = 1
    is_final: bool = False
    status: str = "draft"
    ducking_threshold: Optional[float] = None
    raw_file_url: Optional[str] = None
    mixed_file_url: Optional[str] = None
    libretime_id: Optional[str] = None

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


@router.post("/{voice_track_id}/upload-to-libretime", response_model=VoiceTrackResponse)
async def upload_voice_track_to_libretime(
    voice_track_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Upload a voice track to LibreTime when ready"""
    result = await db.execute(select(VoiceTrack).where(VoiceTrack.id == voice_track_id))
    voice_track = result.scalar_one_or_none()
    
    if not voice_track:
        raise HTTPException(status_code=404, detail="Voice track not found")
    
    # Check if already uploaded
    if voice_track.libretime_id:
        raise HTTPException(
            status_code=400,
            detail=f"Voice track already uploaded to LibreTime (ID: {voice_track.libretime_id})"
        )
    
    # Extract filename from file_url
    filename = voice_track.file_url.split('/')[-2] if '/' in voice_track.file_url else None
    if not filename:
        raise HTTPException(status_code=400, detail="Invalid file URL format")
    
    file_path = Path(VOICE_TRACKS_DIR) / filename
    
    if not file_path.exists():
        raise HTTPException(status_code=404, detail="Voice track file not found")
    
    try:
        # Upload to LibreTime
        result = await libretime_client.upload_voice_track(
            file_path=str(file_path),
            title=voice_track.show_name or f"Voice Track {voice_track_id}",
            description=f"Voice track uploaded from LibreLog",
            artist_name=None,
            genre=None
        )
        
        if result and result.get("success"):
            # Store LibreTime file ID (we'll need to get it from the response or query)
            # For now, we'll store the library_id as a reference
            library_id = result.get("library_id")
            if library_id:
                # Note: The actual file ID will be available after analyzer processes it
                # We might need to query for it later or store a temporary reference
                voice_track.libretime_id = f"lib_{library_id}"
            else:
                voice_track.libretime_id = "uploaded"  # Temporary marker
            
            await db.commit()
            await db.refresh(voice_track)
            
            logger.info(
                "Voice track uploaded to LibreTime",
                voice_track_id=voice_track_id,
                libretime_id=voice_track.libretime_id
            )
            
            return VoiceTrackResponse.model_validate(voice_track)
        else:
            raise HTTPException(
                status_code=500,
                detail="Failed to upload to LibreTime: " + result.get("error", "Unknown error")
            )
            
    except Exception as e:
        logger.error(
            "Failed to upload voice track to LibreTime",
            voice_track_id=voice_track_id,
            error=str(e),
            exc_info=True
        )
        raise HTTPException(
            status_code=500,
            detail=f"Failed to upload to LibreTime: {str(e)}"
        )


# New endpoints for voice tracking features

@router.get("/breaks/{break_id}/preview")
async def get_break_preview(
    break_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get preview information for a break (previous/next tracks)"""
    result = await db.execute(
        select(VoiceTrackSlot).where(VoiceTrackSlot.id == break_id)
    )
    slot = result.scalar_one_or_none()
    
    if not slot:
        raise HTTPException(status_code=404, detail="Break slot not found")
    
    preview_data = {
        "slot_id": slot.id,
        "hour": slot.hour,
        "break_position": slot.break_position,
        "ramp_time": float(slot.ramp_time) if slot.ramp_time else None,
        "previous_track": None,
        "next_track": None
    }
    
    # Get previous track info
    if slot.previous_track_id:
        prev_result = await db.execute(
            select(Track).where(Track.id == slot.previous_track_id)
        )
        prev_track = prev_result.scalar_one_or_none()
        if prev_track:
            preview_service = AudioPreviewService()
            metadata = preview_service.extract_track_metadata(prev_track.filepath)
            preview_data["previous_track"] = {
                "id": prev_track.id,
                "title": prev_track.title,
                "artist": prev_track.artist,
                "duration": metadata.get("duration"),
                "filepath": prev_track.filepath
            }
    
    # Get next track info
    if slot.next_track_id:
        next_result = await db.execute(
            select(Track).where(Track.id == slot.next_track_id)
        )
        next_track = next_result.scalar_one_or_none()
        if next_track:
            preview_service = AudioPreviewService()
            metadata = preview_service.extract_track_metadata(next_track.filepath)
            ramp_data = preview_service.calculate_ramp_times(next_track.filepath)
            preview_data["next_track"] = {
                "id": next_track.id,
                "title": next_track.title,
                "artist": next_track.artist,
                "duration": metadata.get("duration"),
                "ramp_in": ramp_data.get("ramp_in"),
                "filepath": next_track.filepath
            }
    
    return preview_data


@router.get("/tracks/{track_id}/waveform")
async def get_track_waveform(
    track_id: int,
    width: int = Query(800, ge=100, le=2000),
    height: int = Query(200, ge=50, le=500),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Generate waveform data for a track"""
    result = await db.execute(select(Track).where(Track.id == track_id))
    track = result.scalar_one_or_none()
    
    if not track:
        raise HTTPException(status_code=404, detail="Track not found")
    
    if not track.filepath or not os.path.exists(track.filepath):
        raise HTTPException(status_code=404, detail="Track file not found")
    
    preview_service = AudioPreviewService()
    waveform_data = preview_service.generate_waveform_data(track.filepath, width, height)
    
    if not waveform_data:
        raise HTTPException(status_code=500, detail="Failed to generate waveform")
    
    return waveform_data


@router.get("/tracks/{track_id}/preview")
async def stream_track_preview(
    track_id: int,
    start: float = Query(0.0, ge=0.0),
    duration: float = Query(30.0, ge=1.0, le=60.0),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Stream audio preview of a track"""
    from fastapi.responses import FileResponse
    
    result = await db.execute(select(Track).where(Track.id == track_id))
    track = result.scalar_one_or_none()
    
    if not track or not track.filepath or not os.path.exists(track.filepath):
        raise HTTPException(status_code=404, detail="Track not found")
    
    # For now, return full file (in future, could trim to preview)
    return FileResponse(
        path=track.filepath,
        media_type="audio/mpeg",
        filename=f"preview_{track_id}.mp3"
    )


@router.post("/breaks/{break_id}/takes", response_model=VoiceTrackResponse, status_code=status.HTTP_201_CREATED)
async def create_take(
    break_id: int,
    file: UploadFile = File(...),
    take_number: Optional[int] = Form(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new take for a break"""
    # Get slot
    result = await db.execute(
        select(VoiceTrackSlot).where(VoiceTrackSlot.id == break_id)
    )
    slot = result.scalar_one_or_none()
    
    if not slot:
        raise HTTPException(status_code=404, detail="Break slot not found")
    
    # Validate file
    allowed_extensions = {'.mp3', '.wav', '.m4a', '.ogg', '.flac', '.aac'}
    file_ext = Path(file.filename).suffix.lower() if file.filename else ''
    
    if file_ext not in allowed_extensions:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid file type. Allowed: {', '.join(allowed_extensions)}"
        )
    
    # Create directory for this break
    break_dir = Path(VOICE_TRACKS_DIR) / str(break_id)
    break_dir.mkdir(parents=True, exist_ok=True)
    
    # Get next take number if not provided
    if take_number is None:
        existing_takes = await db.execute(
            select(VoiceTrack).where(
                VoiceTrack.break_id == break_id
            ).order_by(VoiceTrack.take_number.desc())
        )
        last_take = existing_takes.scalar_one_or_none()
        take_number = (last_take.take_number + 1) if last_take else 1
    
    # Generate filename
    timestamp = datetime.now().strftime("%Y%m%d-%H%M")
    filename = f"{timestamp}_break{break_id}_take{take_number}{file_ext}"
    file_path = break_dir / filename
    
    # Save file
    try:
        async with aiofiles.open(file_path, 'wb') as f:
            content = await file.read()
            await f.write(content)
        
        file_url = f"/api/voice/{break_id}/{filename}/file"
        
        # Create voice track record
        voice_track = VoiceTrack(
            show_name=f"Break {break_id} - Take {take_number}",
            file_url=file_url,
            uploaded_by=current_user.id,
            break_id=break_id,
            take_number=take_number,
            is_final=False,
            status=VoiceTrackStatus.DRAFT,
            raw_file_url=file_url
        )
        
        db.add(voice_track)
        await db.commit()
        await db.refresh(voice_track)
        
        # Create audit log
        audit = VoiceTrackAudit(
            voice_track_id=voice_track.id,
            action="recorded",
            user_id=current_user.id,
            audit_metadata={"take_number": take_number, "break_id": break_id}
        )
        db.add(audit)
        await db.commit()
        
        logger.info("Take created", break_id=break_id, take_number=take_number, voice_track_id=voice_track.id)
        
        return VoiceTrackResponse.model_validate(voice_track)
        
    except Exception as e:
        logger.error("Take creation failed", error=str(e), exc_info=True)
        if file_path.exists():
            file_path.unlink()
        raise HTTPException(
            status_code=500,
            detail=f"Failed to create take: {str(e)}"
        )


@router.get("/breaks/{break_id}/takes", response_model=list[VoiceTrackResponse])
async def list_takes(
    break_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all takes for a break"""
    result = await db.execute(
        select(VoiceTrack).where(VoiceTrack.break_id == break_id)
        .order_by(VoiceTrack.take_number)
    )
    takes = result.scalars().all()
    
    return [VoiceTrackResponse.model_validate(take) for take in takes]


@router.put("/takes/{take_id}/select", response_model=VoiceTrackResponse)
async def select_take(
    take_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Mark a take as final/selected"""
    result = await db.execute(select(VoiceTrack).where(VoiceTrack.id == take_id))
    voice_track = result.scalar_one_or_none()
    
    if not voice_track:
        raise HTTPException(status_code=404, detail="Voice track not found")
    
    # Unset other takes as final
    if voice_track.break_id:
        await db.execute(
            select(VoiceTrack).where(
                VoiceTrack.break_id == voice_track.break_id,
                VoiceTrack.id != take_id
            )
        )
        other_takes = await db.execute(
            select(VoiceTrack).where(
                VoiceTrack.break_id == voice_track.break_id,
                VoiceTrack.id != take_id
            )
        )
        for other in other_takes.scalars().all():
            other.is_final = False
    
    # Set this take as final
    voice_track.is_final = True
    voice_track.status = VoiceTrackStatus.APPROVED
    
    await db.commit()
    await db.refresh(voice_track)
    
    # Create audit log
    audit = VoiceTrackAudit(
        voice_track_id=voice_track.id,
        action="approved",
        user_id=current_user.id,
        audit_metadata={"selected_as_final": True}
    )
    db.add(audit)
    await db.commit()
    
    logger.info("Take selected as final", take_id=take_id)
    
    return VoiceTrackResponse.model_validate(voice_track)


@router.get("/recordings/production", response_model=list[VoiceTrackResponse])
async def get_production_recordings(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Get all recordings for the current user (production context)
    """
    try:
        result = await db.execute(
            select(VoiceTrack)
            .where(VoiceTrack.uploaded_by == current_user.id)
            .order_by(VoiceTrack.created_at.desc())
        )
        recordings = result.scalars().all()
        return [VoiceTrackResponse.model_validate(r) for r in recordings]
    except Exception as e:
        logger.error("Failed to fetch production recordings", error=str(e), exc_info=True)
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=str(e))


@router.delete("/recordings/{recording_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_recording(
    recording_id: int,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Delete a recording (only if owned by current user)
    """
    try:
        result = await db.execute(
            select(VoiceTrack).where(
                VoiceTrack.id == recording_id,
                VoiceTrack.uploaded_by == current_user.id
            )
        )
        recording = result.scalar_one_or_none()
        
        if not recording:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Recording not found")
        
        # Delete file if exists
        if recording.file_url:
            try:
                file_path = Path(recording.file_url)
                if file_path.exists():
                    file_path.unlink()
            except Exception as e:
                logger.warning("Failed to delete file", file=recording.file_url, error=str(e))
        
        await db.execute(delete(VoiceTrack).where(VoiceTrack.id == recording_id))
        await db.commit()
        
        logger.info("Recording deleted", recording_id=recording_id, user_id=current_user.id)
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error("Failed to delete recording", error=str(e), exc_info=True)
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=str(e))


@router.post("/recordings/standalone", response_model=VoiceTrackResponse, status_code=status.HTTP_201_CREATED)
async def create_standalone_recording(
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Create a standalone test recording (not tied to a break)
    """
    try:
        # Save file
        file_ext = Path(file.filename).suffix or ".webm"
        filename = f"standalone_{current_user.id}_{int(datetime.now().timestamp())}{file_ext}"
        file_path = Path(VOICE_TRACKS_DIR) / filename
        
        async with aiofiles.open(file_path, 'wb') as f:
            content = await file.read()
            await f.write(content)
        
        # Create voice track record (without break_id)
        voice_track = VoiceTrack(
            show_name=f"Test Recording - {current_user.username}",
            file_url=str(file_path),
            raw_file_url=str(file_path),
            scheduled_time=datetime.now(),
            uploaded_by=current_user.id,
            status=VoiceTrackStatus.DRAFT,
            track_metadata={"type": "standalone_test", "user_id": current_user.id}
        )
        
        db.add(voice_track)
        await db.commit()
        await db.refresh(voice_track)
        
        # Create audit log
        audit = VoiceTrackAudit(
            voice_track_id=voice_track.id,
            action="recorded",
            user_id=current_user.id,
            audit_metadata={"type": "standalone_test"}
        )
        db.add(audit)
        await db.commit()
        
        logger.info("Standalone recording created", voice_track_id=voice_track.id, user_id=current_user.id)
        
        return VoiceTrackResponse.model_validate(voice_track)
        
    except Exception as e:
        logger.error("Standalone recording failed", error=str(e), exc_info=True)
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=str(e))


@router.post("/breaks/{break_id}/record", response_model=VoiceTrackResponse, status_code=status.HTTP_201_CREATED)
async def record_voice_track(
    break_id: int,
    file: UploadFile = File(...),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Record voice track directly (streaming upload)"""
    # Similar to create_take but for direct recording
    return await create_take(break_id, file, None, db, current_user)
