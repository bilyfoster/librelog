"""
Celery tasks for async audio processing
"""

from celery import shared_task
from pathlib import Path
import os
import asyncio
from backend.services.audio_processing_service import AudioProcessingService
from backend.integrations.libretime_client import libretime_client
from sqlalchemy import create_engine, select
from sqlalchemy.orm import sessionmaker, Session
from backend.models.voice_track import VoiceTrack
import structlog

logger = structlog.get_logger()
audio_processor = AudioProcessingService()


def get_sync_db():
    """Get synchronous database session for Celery tasks"""
    database_url = os.getenv("POSTGRES_URI", "postgresql://librelog:password@db:5432/librelog")
    # Remove asyncpg from URL for sync connection
    if "+asyncpg" in database_url:
        database_url = database_url.replace("+asyncpg", "")
    engine = create_engine(database_url)
    SessionLocal = sessionmaker(bind=engine)
    return SessionLocal()


@shared_task
def process_voice_track_ducking(voice_track_id: int, music_track_path: str):
    """
    Process ducking for a voice track
    
    Args:
        voice_track_id: Voice track ID
        music_track_path: Path to music bed
    """
    db = None
    try:
        db = get_sync_db()
        
        result = db.execute(select(VoiceTrack).where(VoiceTrack.id == voice_track_id))
        voice_track = result.scalar_one_or_none()
        
        if not voice_track:
            logger.error("Voice track not found", voice_track_id=voice_track_id)
            return
        
        # Get file paths
        voice_file_path = Path(voice_track.raw_file_url or voice_track.file_url)
        if not voice_file_path.is_absolute():
            # Assume it's in VOICE_TRACKS_DIR
            voice_file_path = Path(os.getenv("VOICE_TRACKS_DIR", "/var/lib/librelog/voice_tracks")) / voice_file_path.name
        
        # Create output path for mixed file
        output_path = voice_file_path.parent / f"{voice_file_path.stem}_mixed.mp3"
        
        # Apply ducking
        success = audio_processor.duck_music_bed(
            str(voice_file_path),
            music_track_path,
            str(output_path),
            threshold=float(voice_track.ducking_threshold or -18.0)
        )
        
        if success:
            # Update voice track with mixed file URL
            voice_track.mixed_file_url = f"/api/voice/{voice_track.id}/mixed/file"
            db.commit()
            logger.info("Ducking processed", voice_track_id=voice_track_id)
        else:
            logger.error("Ducking failed", voice_track_id=voice_track_id)
            
    except Exception as e:
        logger.error("Ducking task failed", error=str(e), exc_info=True)
        if db:
            db.rollback()
    finally:
        if db:
            db.close()


@shared_task
def prepare_and_upload_to_libretime(voice_track_id: int):
    """
    Prepare voice track for LibreTime and upload
    
    Args:
        voice_track_id: Voice track ID
    """
    db = None
    try:
        db = get_sync_db()
        
        result = db.execute(select(VoiceTrack).where(VoiceTrack.id == voice_track_id))
        voice_track = result.scalar_one_or_none()
        
        if not voice_track:
            logger.error("Voice track not found", voice_track_id=voice_track_id)
            return
        
        # Check if already uploaded
        if voice_track.libretime_id:
            logger.info("Already uploaded to LibreTime", voice_track_id=voice_track_id)
            return
        
        # Get file path (use mixed if available, otherwise raw)
        file_path = voice_track.mixed_file_url or voice_track.raw_file_url or voice_track.file_url
        if not file_path:
            logger.error("No file path found", voice_track_id=voice_track_id)
            return
        
        # Convert to absolute path
        if not Path(file_path).is_absolute():
            file_path = Path(os.getenv("VOICE_TRACKS_DIR", "/var/lib/librelog/voice_tracks")) / Path(file_path).name
        
        file_path = str(file_path)
        
        if not os.path.exists(file_path):
            logger.error("File not found", file_path=file_path)
            return
        
        # Prepare metadata
        metadata = {
            "title": voice_track.show_name or f"Voice Track {voice_track_id}",
            "artist": "LibreLog Voice Track",  # Could get from user
            "genre": "Voice Over Track"
        }
        
        # Prepare file for LibreTime (encode to MP3 320kbps CBR with ID3 tags)
        prepared_path = audio_processor.prepare_for_libretime_upload(
            file_path,
            metadata
        )
        
        if not prepared_path:
            logger.error("LibreTime preparation failed", voice_track_id=voice_track_id)
            return
        
        # Verify compatibility
        verification = audio_processor.verify_libretime_compatibility(prepared_path)
        if not verification.get("compatible"):
            logger.error("File not compatible with LibreTime", errors=verification.get("errors"))
            return
        
        # Upload to LibreTime (run async function in sync context)
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        try:
            result = loop.run_until_complete(
                libretime_client.upload_voice_track(
                    file_path=prepared_path,
                    title=metadata["title"],
                    description=f"Voice track from LibreLog - Break {voice_track.break_id}",
                    artist_name=metadata["artist"],
                    genre=metadata["genre"]
                )
            )
        finally:
            loop.close()
        
        if result and result.get("success"):
            # Store LibreTime ID
            library_id = result.get("library_id")
            if library_id:
                voice_track.libretime_id = f"lib_{library_id}"
            else:
                voice_track.libretime_id = "uploaded"
            
            voice_track.status = "ready"
            db.commit()
            
            logger.info("Uploaded to LibreTime", voice_track_id=voice_track_id, libretime_id=voice_track.libretime_id)
        else:
            logger.error("LibreTime upload failed", voice_track_id=voice_track_id, result=result)
            
    except Exception as e:
        logger.error("LibreTime upload task failed", error=str(e), exc_info=True)
        if db:
            db.rollback()
    finally:
        if db:
            db.close()

