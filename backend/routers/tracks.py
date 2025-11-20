"""
Tracks router
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from fastapi.responses import StreamingResponse, RedirectResponse
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func
from typing import Optional
from backend.database import get_db
from backend.models.track import Track
from backend.schemas.track import TrackCreate, TrackUpdate, TrackResponse
import os
import httpx

router = APIRouter()


@router.get("/count")
async def get_tracks_count(
    track_type: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db)
):
    """Get total count of tracks - fast endpoint for checking if tracks exist"""
    query = select(func.count(Track.id))
    
    if track_type:
        query = query.where(Track.type == track_type)
    
    result = await db.execute(query)
    count = result.scalar() or 0
    
    return {"count": count}


@router.get("/", response_model=list[TrackResponse])
async def list_tracks(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    track_type: Optional[str] = Query(None),
    search: Optional[str] = Query(None),  # Text search in title/artist
    db: AsyncSession = Depends(get_db)
):
    """List all tracks with optional filtering by type and text search"""
    import structlog
    logger = structlog.get_logger()
    
    # Log received parameters for debugging
    logger.info("list_tracks called", skip=skip, limit=limit, track_type=track_type, search=search)
    
    # Optimize query - only select needed columns if possible
    query = select(Track)
    
    if track_type:
        query = query.where(Track.type == track_type)
    
    # Text search in title and artist
    if search:
        search_term = f"%{search}%"
        from sqlalchemy import or_
        query = query.where(
            or_(
                Track.title.ilike(search_term),
                Track.artist.ilike(search_term)
            )
        )
    
    # Use indexed column for ordering
    query = query.offset(skip).limit(limit).order_by(Track.title)
    
    result = await db.execute(query)
    tracks = result.scalars().all()
    
    logger.info("Found tracks", count=len(tracks))
    
    # Validate and serialize tracks
    try:
        validated_tracks = [TrackResponse.model_validate(track) for track in tracks]
        logger.info("Successfully validated tracks", count=len(validated_tracks))
        return validated_tracks
    except Exception as e:
        logger.error("Error validating tracks", error=str(e), error_type=type(e).__name__, exc_info=True)
        # Log first track that fails validation for debugging
        if tracks:
            first_track = tracks[0]
            logger.error("First track data", 
                        id=first_track.id,
                        title=first_track.title,
                        artist=first_track.artist,
                        type=first_track.type,
                        filepath=first_track.filepath,
                        has_filepath=bool(first_track.filepath))
        raise


@router.post("/", response_model=TrackResponse, status_code=status.HTTP_201_CREATED)
async def create_track(
    track: TrackCreate,
    db: AsyncSession = Depends(get_db)
):
    """Create a new track"""
    # Validate track type
    valid_types = ['MUS', 'ADV', 'PSA', 'LIN', 'INT', 'PRO', 'SHO', 'IDS', 'COM', 'NEW']
    if track.type not in valid_types:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid track type. Must be one of: {', '.join(valid_types)}"
        )
    
    # Check if libretime_id already exists (if provided)
    if track.libretime_id:
        existing = await db.execute(
            select(Track).where(Track.libretime_id == track.libretime_id)
        )
        if existing.scalar_one_or_none():
            raise HTTPException(
                status_code=400,
                detail=f"Track with libretime_id '{track.libretime_id}' already exists"
            )
    
    # Create new track
    new_track = Track(**track.model_dump())
    db.add(new_track)
    await db.commit()
    await db.refresh(new_track)
    
    return TrackResponse.model_validate(new_track)


@router.get("/{track_id}", response_model=TrackResponse)
async def get_track(
    track_id: int,
    db: AsyncSession = Depends(get_db)
):
    """Get a specific track"""
    result = await db.execute(select(Track).where(Track.id == track_id))
    track = result.scalar_one_or_none()
    
    if not track:
        raise HTTPException(status_code=404, detail="Track not found")
    
    return TrackResponse.model_validate(track)


@router.put("/{track_id}", response_model=TrackResponse)
async def update_track(
    track_id: int,
    track: TrackUpdate,
    db: AsyncSession = Depends(get_db)
):
    """Update a track"""
    # Find track
    result = await db.execute(select(Track).where(Track.id == track_id))
    existing_track = result.scalar_one_or_none()
    
    if not existing_track:
        raise HTTPException(status_code=404, detail="Track not found")
    
    # Validate track type if provided
    if track.type:
        valid_types = ['MUS', 'ADV', 'PSA', 'LIN', 'INT', 'PRO', 'SHO', 'IDS', 'COM', 'NEW']
        if track.type not in valid_types:
            raise HTTPException(
                status_code=400,
                detail=f"Invalid track type. Must be one of: {', '.join(valid_types)}"
            )
    
    # Check if libretime_id already exists (if being changed)
    if track.libretime_id and track.libretime_id != existing_track.libretime_id:
        existing = await db.execute(
            select(Track).where(Track.libretime_id == track.libretime_id)
        )
        if existing.scalar_one_or_none():
            raise HTTPException(
                status_code=400,
                detail=f"Track with libretime_id '{track.libretime_id}' already exists"
            )
    
    # Update fields
    update_data = track.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(existing_track, field, value)
    
    await db.commit()
    await db.refresh(existing_track)
    
    return TrackResponse.model_validate(existing_track)


@router.delete("/{track_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_track(
    track_id: int,
    db: AsyncSession = Depends(get_db)
):
    """Delete a track"""
    # Find track
    result = await db.execute(select(Track).where(Track.id == track_id))
    track = result.scalar_one_or_none()
    
    if not track:
        raise HTTPException(status_code=404, detail="Track not found")
    
    # Delete track
    await db.delete(track)
    await db.commit()
    
    return None


@router.get("/{track_id}/preview")
@router.head("/{track_id}/preview")
async def preview_track(
    track_id: int,
    db: AsyncSession = Depends(get_db)
):
    """
    Preview/play a track - redirects to LibreTime file download.
    This endpoint is public (no auth required) as it only redirects to LibreTime.
    """
    result = await db.execute(select(Track).where(Track.id == track_id))
    track = result.scalar_one_or_none()
    
    if not track:
        raise HTTPException(status_code=404, detail="Track not found")
    
    # Get LibreTime URL from environment
    libretime_url = os.getenv("LIBRETIME_URL", "").rstrip("/api/v2").rstrip("/")
    libretime_api_key = os.getenv("LIBRETIME_API_KEY", "")
    
    if not libretime_url:
        raise HTTPException(
            status_code=500,
            detail="LibreTime URL not configured"
        )
    
    import structlog
    import logging
    logger = structlog.get_logger()
    std_logger = logging.getLogger(__name__)
    
    std_logger.info(f"Preview track request: track_id={track_id}, libretime_id={track.libretime_id}, filepath={track.filepath}, libretime_url={libretime_url}")
    logger.info("Preview track request", track_id=track_id, libretime_id=track.libretime_id, filepath=track.filepath, libretime_url=libretime_url)
    
    # Construct URL to LibreTime media file
    # LibreTime typically serves files at /api/v2/files/{id}/download or similar
    # For now, try to construct a direct file URL
    # If the filepath is absolute, we might need to proxy it
    
    if track.libretime_id:
        # Try to get file from LibreTime API
        try:
            # Construct download URL - LibreTime has a download endpoint
            download_url = f"{libretime_url}/api/v2/files/{track.libretime_id}/download"
            logger.info("Attempting to download track from LibreTime", track_id=track_id, libretime_id=track.libretime_id, url=download_url)
            
            # Proxy the file from LibreTime
            async with httpx.AsyncClient() as client:
                headers = {}
                if libretime_api_key:
                    headers["Authorization"] = f"Api-Key {libretime_api_key}"
                
                response = await client.get(
                    download_url,
                    headers=headers,
                    timeout=30.0,
                    follow_redirects=True
                )
                
                logger.info("LibreTime download response", status=response.status_code, headers=dict(response.headers))
                
                # LibreTime uses X-Accel-Redirect which is handled by Nginx
                # We need to construct the media URL from the filepath
                # The filepath should be relative to LibreTime's media directory
                if response.status_code in [200, 302, 307, 308]:
                    # LibreTime uses X-Accel-Redirect for Nginx internal routing
                    # The X-Accel-Redirect path is an internal Nginx path, not directly accessible
                    # We need to use the filepath to construct a direct access URL
                    accel_redirect = response.headers.get("X-Accel-Redirect")
                    std_logger.info(f"X-Accel-Redirect: {accel_redirect}")
                    
                    if accel_redirect:
                        # X-Accel-Redirect format: /api/_media/{filepath}
                        # This is an internal Nginx path - we can't access it directly from another container
                        # Instead, we need to stream the file through our API
                        # The best approach is to use the filepath to access the file if we share volumes
                        # Or construct a URL that LibreTime's web server can serve
                        std_logger.info(f"Got X-Accel-Redirect: {accel_redirect}, filepath: {track.filepath}")
                        
                        # Try to access the file through LibreTime's web interface
                        # LibreTime might serve files at a public endpoint
                        # Check if we have a public URL configured
                        public_url = os.getenv("LIBRETIME_PUBLIC_URL", "")
                        if public_url:
                            # Use public URL to access the file
                            public_download_url = f"{public_url.rstrip('/api/v2').rstrip('/')}/api/v2/files/{track.libretime_id}/download"
                            std_logger.info(f"Redirecting to public LibreTime URL: {public_download_url}")
                            return RedirectResponse(url=public_download_url)
                        
                        # If no public URL, try to access file directly from filesystem if volumes are shared
                        # This would require both containers to mount the same volume
                        # For now, we'll need to configure LIBRETIME_PUBLIC_URL
                        std_logger.warning("X-Accel-Redirect received but no public URL configured - cannot serve file")
                        pass
                    
                    # If we have direct content, use it
                    if response.status_code == 200 and response.content and len(response.content) > 0:
                        std_logger.info(f"Direct file response, content length: {len(response.content)}")
                        return StreamingResponse(
                            iter([response.content]),
                            media_type=response.headers.get("content-type", "audio/mpeg"),
                            headers={
                                "Content-Disposition": f'inline; filename="{track.title}.mp3"',
                                "Accept-Ranges": "bytes",
                            }
                        )
                    else:
                        std_logger.warning(f"Response has no content, status={response.status_code}, content_length={len(response.content) if response.content else 0}")
                else:
                    std_logger.warning(f"LibreTime download failed, status={response.status_code}, url={download_url}")
        except Exception as e:
            # If API download fails, log and try alternative
            logger.error("Failed to preview track via API", track_id=track_id, libretime_id=track.libretime_id, error=str(e), exc_info=True)
            
            # Try direct media URL construction from filepath
            # Since X-Accel-Redirect doesn't work cross-container, try alternative approaches
            if track.filepath:
                try:
                    # Option 1: Try the download endpoint again but this time we know it returns X-Accel-Redirect
                    # We need to access the file through a shared volume or different endpoint
                    # For now, let's try constructing a URL that might work
                    std_logger.info(f"Trying filepath-based access: {track.filepath}")
                    
                    # Check if we can access files through a shared volume
                    # This would require both containers to have access to the same volume
                    # For now, return a redirect to LibreTime's public URL if available
                    # Or construct a proxy URL
                    
                    # Since we can't directly access X-Accel-Redirect paths, we'll need to
                    # either share volumes or use a different approach
                    # For now, return an error with instructions
                    pass
                except Exception as e2:
                    std_logger.error(f"Failed to preview track via direct URL: {e2}", exc_info=True)
                    logger.error("Failed to preview track via direct URL", track_id=track_id, error=str(e2), exc_info=True)
    else:
        logger.warning("Track has no libretime_id", track_id=track_id, filepath=track.filepath)
    
    # Since X-Accel-Redirect paths are internal to LibreTime's Nginx and not accessible
    # from other containers, we need to either:
    # 1. Share the media volume between containers
    # 2. Use LibreTime's public media URL (if configured)
    # 3. Proxy through LibreTime's web interface
    
    # For now, return a redirect to LibreTime's download endpoint
    # The browser can follow this redirect
    std_logger.info(f"Returning redirect to LibreTime for track {track_id}")
    
    if track.libretime_id:
        # Proxy the file from LibreTime to avoid authentication issues in browser
        # Prefer internal URL (container name) for server-to-server communication
        # Fall back to public URL if internal URL not configured
        internal_url = os.getenv("LIBRETIME_INTERNAL_URL", "")  # e.g., http://nginx:8080
        api_url = internal_url if internal_url else os.getenv("LIBRETIME_API_URL", libretime_url)
        if not api_url:
            api_url = os.getenv("LIBRETIME_URL", "").rstrip("/api/v2").rstrip("/")
        
        if api_url and libretime_api_key:
            # Clean up the URL
            api_url = api_url.rstrip('/api/v2').rstrip('/')
            download_url = f"{api_url}/api/v2/files/{track.libretime_id}/download"
            std_logger.info(f"Proxying track from LibreTime: {download_url}")
            
            try:
                # Fetch file from LibreTime with API key
                async with httpx.AsyncClient(timeout=60.0, follow_redirects=True) as client:
                    headers = {"Authorization": f"Api-Key {libretime_api_key}"}
                    response = await client.get(download_url, headers=headers)
                    
                    if response.status_code == 200:
                        # Determine content type from response or file extension
                        content_type = response.headers.get("content-type", "")
                        # If content-type is HTML, it might be a redirect page - check file extension
                        if "text/html" in content_type or not content_type:
                            # Determine from filepath or default to audio/mpeg
                            if track.filepath and track.filepath.endswith((".mp3", ".MP3")):
                                content_type = "audio/mpeg"
                            elif track.filepath and track.filepath.endswith((".ogg", ".OGG")):
                                content_type = "audio/ogg"
                            elif track.filepath and track.filepath.endswith((".wav", ".WAV")):
                                content_type = "audio/wav"
                            else:
                                content_type = "audio/mpeg"  # Default
                        
                        # Clean up content-type (remove charset if present for audio)
                        if "charset" in content_type:
                            content_type = content_type.split(";")[0].strip()
                        
                        # Stream the file to the browser
                        return StreamingResponse(
                            iter([response.content]),
                            media_type=content_type,
                            headers={
                                "Content-Disposition": f'inline; filename="{track.title or "track"}.mp3"',
                                "Accept-Ranges": "bytes",
                                "Content-Length": str(len(response.content)),
                            }
                        )
                    else:
                        std_logger.error(f"LibreTime returned status {response.status_code}")
                        raise HTTPException(
                            status_code=response.status_code,
                            detail=f"Failed to fetch track from LibreTime: {response.status_code}"
                        )
            except Exception as e:
                std_logger.error(f"Error proxying track: {e}", exc_info=True)
                raise HTTPException(
                    status_code=500,
                    detail=f"Failed to proxy track from LibreTime: {str(e)}"
                )
        else:
            std_logger.error("LibreTime API URL or API key not configured")
            raise HTTPException(
                status_code=501,
                detail="Preview requires LIBRETIME_API_URL and LIBRETIME_API_KEY to be configured"
            )
    
    logger.error("All preview methods failed", track_id=track_id, libretime_id=track.libretime_id, filepath=track.filepath)
    raise HTTPException(
        status_code=501,
        detail=f"Preview not available. Track ID: {track_id}, LibreTime ID: {track.libretime_id or 'None'}, Filepath: {track.filepath or 'None'}"
    )


@router.get("/preview-by-path")
async def preview_track_by_path(
    path: str = Query(..., description="File path to preview"),
    db: AsyncSession = Depends(get_db)
):
    """
    Preview a track by file path - finds the track and redirects to preview
    """
    result = await db.execute(select(Track).where(Track.filepath == path))
    track = result.scalar_one_or_none()
    
    if not track:
        raise HTTPException(status_code=404, detail=f"Track not found for path: {path}")
    
    # Redirect to the track preview endpoint
    return RedirectResponse(url=f"/api/tracks/{track.id}/preview", status_code=302)
