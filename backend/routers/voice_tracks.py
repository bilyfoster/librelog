"""
Voice tracks router
"""

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from backend.database import get_db
from backend.models.voice_track import VoiceTrack

router = APIRouter()


@router.get("/")
async def list_voice_tracks(
    skip: int = 0,
    limit: int = 100,
    db: AsyncSession = Depends(get_db)
):
    """List all voice tracks"""
    # TODO: Implement voice track listing
    return {"voice_tracks": [], "total": 0}


@router.post("/upload")
async def upload_voice_track(
    file: bytes,
    show_name: str = None,
    db: AsyncSession = Depends(get_db)
):
    """Upload a voice track"""
    # TODO: Implement voice track upload
    return {"message": "Voice track uploaded successfully"}


@router.get("/{voice_track_id}")
async def get_voice_track(
    voice_track_id: int,
    db: AsyncSession = Depends(get_db)
):
    """Get a specific voice track"""
    # TODO: Implement voice track retrieval
    raise HTTPException(status_code=404, detail="Voice track not found")


@router.delete("/{voice_track_id}")
async def delete_voice_track(
    voice_track_id: int,
    db: AsyncSession = Depends(get_db)
):
    """Delete a voice track"""
    # TODO: Implement voice track deletion
    raise HTTPException(status_code=404, detail="Voice track not found")
