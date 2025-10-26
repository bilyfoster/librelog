from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File, Form
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.models.artist import Artist
from app.models.submission import Submission
from app.services.storage import storage_service
from app.services.email import email_service
from app.services.isrc_generator import generate_isrc_code
from app.schemas.submission import SubmissionCreate, SubmissionResponse, SubmissionStatus
from app.core.security import get_current_artist
from pydantic import BaseModel, EmailStr
from typing import Optional, List
import uuid
from datetime import datetime
import re
import json
import os

router = APIRouter()


class ArtistCreate(BaseModel):
    email: EmailStr
    name: str
    pronouns: Optional[str] = None
    bio: Optional[str] = None
    social_links: Optional[dict] = {}


class SubmissionCreate(BaseModel):
    artist: ArtistCreate
    song_title: str
    genre: Optional[str] = None
    file_name: str
    file_size: int
    isrc_requested: bool = False
    radio_permission: bool = False
    public_display: bool = False
    rights_attestation: bool = False
    pro_info: Optional[dict] = {}


class SubmissionResponse(BaseModel):
    id: str
    tracking_id: str
    status: str
    song_title: str
    genre: Optional[str]
    submitted_at: datetime
    isrc_requested: bool
    radio_permission: bool
    public_display: bool
    artist: dict

    class Config:
        from_attributes = True


class UploadResponse(BaseModel):
    upload_url: str
    object_name: str
    tracking_id: str


@router.post("/", response_model=UploadResponse)
async def create_submission(
    submission_data: SubmissionCreate,
    db: Session = Depends(get_db)
):
    """Create a new submission and return presigned upload URL"""
    
    # Validate file size (150MB max)
    if submission_data.file_size > 150 * 1024 * 1024:
        raise HTTPException(
            status_code=400,
            detail="File size exceeds 150MB limit"
        )
    
    # Validate file extension - MP3 only
    file_ext = submission_data.file_name.lower().split('.')[-1]
    if file_ext != 'mp3':
        raise HTTPException(
            status_code=400,
            detail="Only MP3 files are accepted. Please convert your audio file to MP3 format."
        )
    
    # Validate rights attestation is checked
    if not submission_data.rights_attestation:
        raise HTTPException(
            status_code=400,
            detail="Rights attestation is required"
        )
    
    # Get or create artist
    artist = db.query(Artist).filter(Artist.email == submission_data.artist.email).first()
    if not artist:
        artist = Artist(
            email=submission_data.artist.email,
            name=submission_data.artist.name,
            pronouns=submission_data.artist.pronouns,
            bio=submission_data.artist.bio,
            social_links=submission_data.artist.social_links or {}
        )
        db.add(artist)
        db.commit()
        db.refresh(artist)
    
    # Generate tracking ID
    from sqlalchemy import text
    result = db.execute(text("SELECT generate_tracking_id()"))
    tracking_id = result.scalar()
    
    # Create submission record
    submission = Submission(
        tracking_id=tracking_id,
        artist_id=artist.id,
        song_title=submission_data.song_title,
        genre=submission_data.genre,
        file_name=submission_data.file_name,
        file_size=submission_data.file_size,
        file_path="",  # Will be set after upload
        isrc_requested=submission_data.isrc_requested,
        radio_permission=submission_data.radio_permission,
        public_display=submission_data.public_display,
        rights_attestation=submission_data.rights_attestation,
        pro_info=submission_data.pro_info or {}
    )
    
    db.add(submission)
    db.commit()
    db.refresh(submission)
    
    # Generate presigned upload URL
    upload_data = storage_service.generate_presigned_upload_url(
        submission_data.file_name,
        content_type="audio/mpeg"
    )
    
    # Update submission with file path
    submission.file_path = upload_data["object_name"]
    db.commit()
    
    return UploadResponse(
        upload_url=upload_data["upload_url"],
        object_name=upload_data["object_name"],
        tracking_id=tracking_id
    )


@router.post("/{submission_id}/upload-complete")
async def mark_upload_complete(
    submission_id: str,
    db: Session = Depends(get_db)
):
    """Mark submission as upload complete and trigger processing"""
    
    submission = db.query(Submission).filter(Submission.id == submission_id).first()
    if not submission:
        raise HTTPException(status_code=404, detail="Submission not found")
    
    # Update file status
    submission.file_status = "ready"
    db.commit()
    
    # Send confirmation email
    email_service.send_submission_confirmation(
        submission.artist.email,
        submission.artist.name,
        submission.tracking_id
    )
    
    return {"message": "Upload completed successfully"}


@router.get("/track/{tracking_id}", response_model=SubmissionResponse)
async def get_submission_by_tracking_id(
    tracking_id: str,
    db: Session = Depends(get_db)
):
    """Get submission by tracking ID (public endpoint)"""
    
    submission = db.query(Submission).filter(Submission.tracking_id == tracking_id).first()
    if not submission:
        raise HTTPException(status_code=404, detail="Submission not found")
    
    return SubmissionResponse(
        id=str(submission.id),
        tracking_id=submission.tracking_id,
        status=submission.status,
        song_title=submission.song_title,
        genre=submission.genre,
        submitted_at=submission.submitted_at,
        isrc_requested=submission.isrc_requested,
        radio_permission=submission.radio_permission,
        public_display=submission.public_display,
        artist={
            "name": submission.artist.name,
            "email": submission.artist.email,
            "pronouns": submission.artist.pronouns
        }
    )


@router.get("/{submission_id}/download-url")
async def get_download_url(
    submission_id: str,
    db: Session = Depends(get_db)
):
    """Get presigned download URL for submission file"""
    
    submission = db.query(Submission).filter(Submission.id == submission_id).first()
    if not submission:
        raise HTTPException(status_code=404, detail="Submission not found")
    
    if not submission.file_path:
        raise HTTPException(status_code=400, detail="File not available")
    
    download_url = storage_service.generate_presigned_download_url(submission.file_path)
    
    return {"download_url": download_url}


@router.get("/", response_model=List[SubmissionResponse])
async def list_submissions(
    status: Optional[str] = None,
    isrc_requested: Optional[bool] = None,
    limit: int = 50,
    offset: int = 0,
    db: Session = Depends(get_db)
):
    """List submissions with optional filtering (admin endpoint)"""
    
    query = db.query(Submission)
    
    if status:
        query = query.filter(Submission.status == status)
    
    if isrc_requested is not None:
        query = query.filter(Submission.isrc_requested == isrc_requested)
    
    submissions = query.order_by(Submission.submitted_at.desc()).offset(offset).limit(limit).all()
    
    return [
        SubmissionResponse(
            id=str(sub.id),
            tracking_id=sub.tracking_id,
            status=sub.status,
            song_title=sub.song_title,
            genre=sub.genre,
            submitted_at=sub.submitted_at,
            isrc_requested=sub.isrc_requested,
            radio_permission=sub.radio_permission,
            public_display=sub.public_display,
            artist={
                "name": sub.artist.name,
                "email": sub.artist.email,
                "pronouns": sub.artist.pronouns
            }
        )
        for sub in submissions
    ]


@router.post("/upload")
async def upload_file(
    file: UploadFile = File(...),
    artist_email: str = Form(...),
    artist_name: str = Form(...),  # Added artist_name parameter
    song_title: str = Form(...),
    genre: Optional[str] = Form(None),
    isrc_requested: bool = Form(False),
    radio_permission: bool = Form(False),
    public_display: bool = Form(False),
    rights_attestation: bool = Form(False),
    db: Session = Depends(get_db)
):
    """Direct file upload endpoint"""
    
    # Validate file size (150MB max)
    if file.size and file.size > 150 * 1024 * 1024:
        raise HTTPException(
            status_code=400,
            detail="File size exceeds 150MB limit"
        )
    
    # Validate file extension - MP3 only
    file_ext = file.filename.lower().split('.')[-1] if '.' in file.filename else ''
    if file_ext != 'mp3':
        raise HTTPException(
            status_code=400,
            detail="Only MP3 files are accepted. Please convert your audio file to MP3 format."
        )
    
    # Validate rights attestation is checked
    if not rights_attestation:
        raise HTTPException(
            status_code=400,
            detail="Rights attestation is required"
        )
    
    # Get or create artist (support multiple artists per email)
    artist = db.query(Artist).filter(
        Artist.email == artist_email,
        Artist.name == artist_name
    ).first()
    
    if not artist:
        # Create new artist profile for this email/name combination
        artist = Artist(
            email=artist_email,
            name=artist_name,
            is_active=True
        )
        db.add(artist)
        db.commit()
        db.refresh(artist)
    
    # Generate tracking ID
    from sqlalchemy import text
    result = db.execute(text("SELECT generate_tracking_id()"))
    tracking_id = result.scalar()
    
    # Upload file to MinIO
    object_name = f"submissions/{uuid.uuid4()}/{file.filename}"
    try:
        # Read file content
        file_content = await file.read()
        
        # Process MP3 metadata
        from app.services.mp3_metadata import mp3_metadata_service
        
        # Get audio properties
        audio_props = mp3_metadata_service.get_audio_properties(file_content)
        
        # Check if file has metadata, if not inject it
        has_metadata = mp3_metadata_service.has_metadata(file_content)
        if not has_metadata:
            # Inject metadata from form data
            metadata = {
                'title': song_title,
                'artist': artist_name,
                'album': f"{artist_name} - Singles",
                'year': str(datetime.now().year),
                'genre': genre or 'Unknown',
                'tracking_id': tracking_id,
                'submission_date': datetime.now().strftime('%Y-%m-%d'),
            }
            
            # Write metadata to file
            file_content = mp3_metadata_service.write_metadata(file_content, metadata)
        
        # Upload to MinIO
        success = storage_service.upload_file_content(
            file_content, 
            object_name, 
            file.content_type or "audio/mpeg"
        )
        
        if not success:
            raise HTTPException(
                status_code=500,
                detail="Failed to upload file"
            )
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Upload failed: {str(e)}"
        )
    
    # Create submission record
    submission = Submission(
        tracking_id=tracking_id,
        artist_id=artist.id,
        artist_name=artist_name,  # Store artist name at submission time
        song_title=song_title,
        genre=genre,
        file_name=file.filename,
        file_size=file.size,
        file_path=object_name,
        isrc_requested=isrc_requested,
        radio_permission=radio_permission,
        public_display=public_display,
        rights_attestation=rights_attestation,
        status="pending",
        # Add audio properties from MP3 processing
        duration_seconds=audio_props.get('duration_seconds', 0),
        bitrate=audio_props.get('bitrate', 0),
        sample_rate=audio_props.get('sample_rate', 0),
        channels=audio_props.get('channels', 0)
    )
    
    db.add(submission)
    db.commit()
    db.refresh(submission)
    
    # Send confirmation email
    email_service.send_submission_confirmation(
        artist.email, 
        artist.name, 
        tracking_id
    )
    
    return {
        "message": "File uploaded successfully",
        "tracking_id": tracking_id,
        "submission_id": str(submission.id)
    }


@router.get("/gallery")
async def get_gallery_artists(db: Session = Depends(get_db)):
    """Get artists with approved public tracks for gallery display"""
    
    # Get artists who have at least one processed submission with public_display = true
    artists_with_public_tracks = db.query(Artist).join(Submission).filter(
        Submission.status == "isrc_assigned",
        Submission.public_display == True
    ).distinct().all()
    
    result = []
    for artist in artists_with_public_tracks:
        # Get processed public submissions for this artist
        public_submissions = db.query(Submission).filter(
            Submission.artist_id == artist.id,
            Submission.status == "isrc_assigned",
            Submission.public_display == True
        ).all()
        
        # Get total submission count (including non-public)
        total_submissions = db.query(Submission).filter(
            Submission.artist_id == artist.id
        ).count()
        
        result.append({
            "id": str(artist.id),
            "name": artist.name,
            "email": artist.email,
            "pronouns": artist.pronouns,
            "bio": artist.bio,
            "social_links": artist.social_links or {},
            "public_track_count": len(public_submissions),
            "total_submission_count": total_submissions,
            "public_submissions": [
                {
                    "id": str(sub.id),
                    "song_title": sub.song_title,
                    "genre": sub.genre,
                    "tracking_id": sub.tracking_id,
                    "created_at": sub.created_at.isoformat()
                }
                for sub in public_submissions
            ]
        })
    
    return result


@router.get("/artists/{email}")
async def get_artists_for_email(
    email: str,
    db: Session = Depends(get_db)
):
    """Get all artist profiles for an email address"""
    
    artists = db.query(Artist).filter(Artist.email == email).all()
    
    return [
        {
            "id": str(artist.id),
            "name": artist.name,
            "email": artist.email,
            "pronouns": artist.pronouns,
            "bio": artist.bio,
            "social_links": artist.social_links or {}
        }
        for artist in artists
    ]


@router.get("/my")
async def get_my_submissions(
    current_artist: Artist = Depends(get_current_artist),
    db: Session = Depends(get_db)
):
    """Get submissions for the current artist"""
    from sqlalchemy.orm import joinedload
    from app.models.isrc import ISRC
    from app.services.libretime_service import LibreTimeService
    
    submissions = db.query(Submission).options(joinedload(Submission.isrc)).filter(Submission.artist_id == current_artist.id).order_by(Submission.created_at.desc()).all()
    
    # Get play statistics service
    service = LibreTimeService(db)
    
    submission_data = []
    for sub in submissions:
        # Get play statistics for this submission
        play_stats = service.get_play_statistics(str(sub.id))
        
        submission_data.append({
            "id": str(sub.id),
            "title": sub.song_title,
            "status": sub.status,
            "tracking_id": sub.tracking_id,
            "created_at": sub.created_at.isoformat(),
            "isrc_code": sub.isrc.isrc_code if sub.isrc else None,
            "play_count": play_stats.get("total_plays", 0) if "error" not in play_stats else 0,
            "last_played_at": play_stats.get("last_played_at") if "error" not in play_stats else None
        })
    
    return submission_data
