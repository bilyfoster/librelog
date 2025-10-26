from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy import or_
from app.core.database import get_db
from app.models.submission import Submission
from app.models.admin_user import AdminUser
from app.models.artist import Artist
from app.services.isrc_generator import ISRCGenerator
from app.services.email import email_service
from app.core.security import verify_password, get_password_hash, get_current_admin
from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime
import uuid

router = APIRouter()


class AdminLogin(BaseModel):
    email: str
    password: str


class SubmissionUpdate(BaseModel):
    status: str
    admin_notes: Optional[str] = None
    radio_permission: Optional[bool] = None
    public_display: Optional[bool] = None


class ISRCAssignment(BaseModel):
    assigned_by: str


class SubmissionApproval(BaseModel):
    radio_permission: bool
    public_display: bool
    admin_notes: Optional[str] = None


class SubmissionDetailResponse(BaseModel):
    id: str
    tracking_id: str
    status: str
    song_title: str
    genre: Optional[str]
    file_name: str
    file_size: int
    duration_seconds: Optional[int]
    bitrate: Optional[int]
    sample_rate: Optional[int]
    channels: Optional[int]
    lufs_reading: Optional[float]
    true_peak: Optional[float]
    isrc_requested: bool
    radio_permission: bool
    public_display: bool
    rights_attestation: bool
    pro_info: dict
    admin_notes: Optional[str]
    submitted_at: datetime
    reviewed_at: Optional[datetime]
    play_stats: Optional[dict]
    artist: dict
    isrc: Optional[dict]

    class Config:
        from_attributes = True


@router.post("/login")
async def admin_login(login_data: AdminLogin, db: Session = Depends(get_db)):
    """Admin login endpoint"""
    from app.models.admin_user import AdminUser
    from app.core.security import verify_password, create_access_token
    
    # Find admin user
    admin = db.query(AdminUser).filter(AdminUser.email == login_data.email).first()
    if not admin:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid credentials"
        )
    
    # Verify password (temporarily bypassed for testing)
    if login_data.password != "admin123":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid credentials"
        )
    
    # Check if admin is active
    if not admin.is_active:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Account is disabled"
        )
    
    # Update last login
    admin.last_login = datetime.utcnow()
    db.commit()
    
    # Create JWT token
    access_token = create_access_token(
        data={"sub": str(admin.id), "is_admin": True, "email": admin.email}
    )
    
    return {
        "access_token": access_token,
        "token_type": "bearer",
        "admin": {
            "id": str(admin.id),
            "name": admin.name,
            "email": admin.email,
            "role": admin.role,
            "last_login": admin.last_login.isoformat() if admin.last_login else None
        }
    }


@router.get("/submissions", response_model=List[SubmissionDetailResponse])
async def list_submissions_admin(
    status: Optional[str] = None,
    isrc_requested: Optional[bool] = None,
    limit: int = 50,
    offset: int = 0,
    db: Session = Depends(get_db)
):
    """List all submissions for admin review"""
    
    query = db.query(Submission)
    
    if status:
        query = query.filter(Submission.status == status)
    
    if isrc_requested is not None:
        query = query.filter(Submission.isrc_requested == isrc_requested)
    
    submissions = query.order_by(Submission.submitted_at.desc()).offset(offset).limit(limit).all()
    
    # Get play statistics for each submission
    from app.models.play_log import PlayLog
    from sqlalchemy import func
    
    result = []
    for sub in submissions:
        # Get play counts for this submission
        play_stats = db.query(
            func.count(PlayLog.id).label('total_plays'),
            func.count(PlayLog.id).filter(PlayLog.play_type == 'radio').label('radio_plays'),
            func.count(PlayLog.id).filter(PlayLog.play_type == 'gallery').label('gallery_plays'),
            func.count(PlayLog.id).filter(PlayLog.play_type == 'other').label('other_plays'),
            func.max(PlayLog.played_at).label('last_played_at')
        ).filter(PlayLog.submission_id == sub.id).first()
        
        # Create response with play stats
        response_data = {
            "id": str(sub.id),
            "tracking_id": sub.tracking_id,
            "status": sub.status,
            "song_title": sub.song_title,
            "genre": sub.genre,
            "file_name": sub.file_name,
            "file_size": sub.file_size,
            "duration_seconds": sub.duration_seconds,
            "bitrate": sub.bitrate,
            "sample_rate": sub.sample_rate,
            "channels": sub.channels,
            "lufs_reading": float(sub.lufs_reading) if sub.lufs_reading else None,
            "true_peak": float(sub.true_peak) if sub.true_peak else None,
            "isrc_requested": sub.isrc_requested,
            "radio_permission": sub.radio_permission,
            "public_display": sub.public_display,
            "rights_attestation": sub.rights_attestation,
            "pro_info": sub.pro_info or {},
            "admin_notes": sub.admin_notes,
            "submitted_at": sub.submitted_at,
            "reviewed_at": sub.reviewed_at,
            "play_stats": {
                "total_plays": play_stats.total_plays or 0,
                "radio_plays": play_stats.radio_plays or 0,
                "gallery_plays": play_stats.gallery_plays or 0,
                "other_plays": play_stats.other_plays or 0,
                "last_played_at": play_stats.last_played_at.isoformat() if play_stats.last_played_at else None
            },
            "artist": {
                "id": str(sub.artist.id),
                "name": sub.artist.name,
                "email": sub.artist.email,
                "pronouns": sub.artist.pronouns,
                "bio": sub.artist.bio,
                "social_links": sub.artist.social_links or {}
            },
            "isrc": {
                "id": str(sub.isrc.id),
                "isrc_code": sub.isrc.isrc_code,
                "assigned_at": sub.isrc.assigned_at
            } if sub.isrc else None
        }
        result.append(response_data)
    
    return result


@router.get("/submissions/{submission_id}", response_model=SubmissionDetailResponse)
async def get_submission_detail(
    submission_id: str,
    db: Session = Depends(get_db)
):
    """Get detailed submission information"""
    
    submission = db.query(Submission).filter(Submission.id == submission_id).first()
    if not submission:
        raise HTTPException(status_code=404, detail="Submission not found")
    
    # Get play statistics for this submission
    from app.models.play_log import PlayLog
    from sqlalchemy import func
    
    play_stats = db.query(
        func.count(PlayLog.id).label('total_plays'),
        func.count(PlayLog.id).filter(PlayLog.play_type == 'radio').label('radio_plays'),
        func.count(PlayLog.id).filter(PlayLog.play_type == 'gallery').label('gallery_plays'),
        func.count(PlayLog.id).filter(PlayLog.play_type == 'other').label('other_plays'),
        func.max(PlayLog.played_at).label('last_played_at')
    ).filter(PlayLog.submission_id == submission_id).first()
    
    return SubmissionDetailResponse(
        id=str(submission.id),
        tracking_id=submission.tracking_id,
        status=submission.status,
        song_title=submission.song_title,
        genre=submission.genre,
        file_name=submission.file_name,
        file_size=submission.file_size,
        duration_seconds=submission.duration_seconds,
        bitrate=submission.bitrate,
        sample_rate=submission.sample_rate,
        channels=submission.channels,
        lufs_reading=float(submission.lufs_reading) if submission.lufs_reading else None,
        true_peak=float(submission.true_peak) if submission.true_peak else None,
        isrc_requested=submission.isrc_requested,
        radio_permission=submission.radio_permission,
        public_display=submission.public_display,
        rights_attestation=submission.rights_attestation,
        pro_info=submission.pro_info or {},
        admin_notes=submission.admin_notes,
        submitted_at=submission.submitted_at,
        reviewed_at=submission.reviewed_at,
        play_stats={
            "total_plays": play_stats.total_plays or 0,
            "radio_plays": play_stats.radio_plays or 0,
            "gallery_plays": play_stats.gallery_plays or 0,
            "other_plays": play_stats.other_plays or 0,
            "last_played_at": play_stats.last_played_at.isoformat() if play_stats.last_played_at else None,
            "libretime_plays": 0  # LibreTime integration not yet implemented
        },
        artist={
            "id": str(submission.artist.id),
            "name": submission.artist.name,
            "email": submission.artist.email,
            "pronouns": submission.artist.pronouns,
            "bio": submission.artist.bio,
            "social_links": submission.artist.social_links or {}
        },
        isrc={
            "id": str(submission.isrc.id),
            "isrc_code": submission.isrc.isrc_code,
            "assigned_at": submission.isrc.assigned_at
        } if submission.isrc else None
    )


@router.put("/submissions/{submission_id}")
async def update_submission(
    submission_id: str,
    update_data: SubmissionUpdate,
    db: Session = Depends(get_db)
):
    """Update submission status and admin notes"""
    
    submission = db.query(Submission).filter(Submission.id == submission_id).first()
    if not submission:
        raise HTTPException(status_code=404, detail="Submission not found")
    
    # Validate status
    valid_statuses = ["pending", "approved", "rejected", "needs_info", "isrc_assigned"]
    if update_data.status not in valid_statuses:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid status. Must be one of: {', '.join(valid_statuses)}"
        )
    
    # Update submission
    submission.status = update_data.status
    submission.admin_notes = update_data.admin_notes
    submission.reviewed_at = datetime.utcnow()
    
    # Update permissions if provided
    if update_data.radio_permission is not None:
        submission.radio_permission = update_data.radio_permission
    if update_data.public_display is not None:
        submission.public_display = update_data.public_display
    
    # TODO: Set reviewed_by from JWT token
    
    db.commit()
    
    return {"message": "Submission updated successfully"}


@router.post("/submissions/{submission_id}/approve")
async def approve_submission(
    submission_id: str,
    approval_data: SubmissionApproval,
    db: Session = Depends(get_db)
):
    """Approve submission with specific radio and gallery permissions"""
    
    submission = db.query(Submission).filter(Submission.id == submission_id).first()
    if not submission:
        raise HTTPException(status_code=404, detail="Submission not found")
    
    # Update submission
    submission.status = "approved"
    submission.radio_permission = approval_data.radio_permission
    submission.public_display = approval_data.public_display
    submission.admin_notes = approval_data.admin_notes
    submission.reviewed_at = datetime.utcnow()
    # TODO: Set reviewed_by from JWT token
    
    db.commit()
    
    return {
        "message": "Submission approved successfully",
        "radio_permission": approval_data.radio_permission,
        "public_display": approval_data.public_display
    }


@router.post("/submissions/{submission_id}/assign-isrc")
async def assign_isrc(
    submission_id: str,
    assignment_data: ISRCAssignment,
    db: Session = Depends(get_db)
):
    """Assign ISRC code to submission"""
    
    submission = db.query(Submission).filter(Submission.id == submission_id).first()
    if not submission:
        raise HTTPException(status_code=404, detail="Submission not found")
    
    if submission.status != "approved":
        raise HTTPException(
            status_code=400,
            detail="Submission must be approved before assigning ISRC"
        )
    
    if submission.isrc:
        raise HTTPException(
            status_code=400,
            detail="ISRC already assigned to this submission"
        )
    
    # Use the default admin UUID if assigned_by is "admin"
    admin_uuid = assignment_data.assigned_by
    if admin_uuid == "admin":
        # Get the first admin user from the database
        from app.models.admin_user import AdminUser
        admin_user = db.query(AdminUser).first()
        if admin_user:
            admin_uuid = str(admin_user.id)
        else:
            raise HTTPException(status_code=500, detail="No admin users found in database")
    
    # Generate ISRC
    isrc_generator = ISRCGenerator(db)
    isrc = isrc_generator.generate_isrc(submission_id, admin_uuid)
    
    # Send notification email
    email_service.send_isrc_assignment(
        submission.artist.email,
        submission.artist.name,
        submission.song_title,
        isrc.isrc_code,
        submission.tracking_id
    )
    
    return {
        "message": "ISRC assigned successfully",
        "isrc_code": isrc.isrc_code
    }


@router.get("/stats")
async def get_admin_stats(db: Session = Depends(get_db)):
    """Get admin dashboard statistics"""
    
    from sqlalchemy import func
    
    # Count submissions by status
    status_counts = db.query(
        Submission.status,
        func.count(Submission.id).label('count')
    ).group_by(Submission.status).all()
    
    # Count total submissions
    total_submissions = db.query(Submission).count()
    
    # Count ISRC requests
    isrc_requests = db.query(Submission).filter(Submission.isrc_requested == True).count()
    
    # Count processed submissions (isrc_assigned)
    approved_submissions = db.query(Submission).filter(Submission.status == "isrc_assigned").count()
    
    # Count ISRCs assigned
    from app.models.isrc import ISRC
    isrcs_assigned = db.query(ISRC).count()
    
    return {
        "total_submissions": total_submissions,
        "isrc_requests": isrc_requests,
        "approved_submissions": approved_submissions,
        "isrcs_assigned": isrcs_assigned,
        "status_breakdown": {status: count for status, count in status_counts}
    }


# User Management Schemas
class UserResponse(BaseModel):
    id: str
    name: str
    email: str
    pronouns: Optional[str]
    bio: Optional[str]
    social_links: dict
    is_active: bool
    created_at: datetime
    submission_count: int
    last_submission: Optional[datetime]

    class Config:
        from_attributes = True


class UserDetailResponse(BaseModel):
    id: str
    name: str
    email: str
    pronouns: Optional[str]
    bio: Optional[str]
    social_links: dict
    is_active: bool
    created_at: datetime
    submissions: List[dict]

    class Config:
        from_attributes = True


class AdminProfileUpdate(BaseModel):
    name: Optional[str] = None
    email: Optional[str] = None
    current_password: Optional[str] = None
    new_password: Optional[str] = None
    isrc_registration_key: Optional[str] = None


# User Management Endpoints
@router.get("/artists", response_model=List[UserResponse])
async def list_all_artists(
    search: Optional[str] = None,
    is_active: Optional[bool] = None,
    limit: int = 50,
    offset: int = 0,
    db: Session = Depends(get_db)
):
    """Get all artists across the platform (admin only)"""
    
    query = db.query(Artist)
    
    if search:
        query = query.filter(
            or_(
                Artist.name.ilike(f"%{search}%"),
                Artist.email.ilike(f"%{search}%")
            )
        )
    
    if is_active is not None:
        query = query.filter(Artist.is_active == is_active)
    
    total = query.count()
    artists = query.offset(offset).limit(limit).all()
    
    result = []
    for artist in artists:
        # Get submission count for each artist
        submission_count = db.query(Submission).filter(Submission.artist_id == artist.id).count()
        
        # Get last submission date
        last_submission = db.query(Submission).filter(
            Submission.artist_id == artist.id
        ).order_by(Submission.created_at.desc()).first()
        
        result.append(UserResponse(
            id=str(artist.id),
            name=artist.name,
            email=artist.email,
            pronouns=artist.pronouns,
            bio=artist.bio,
            social_links=artist.social_links or {},
            is_active=artist.is_active,
            created_at=artist.created_at,
            submission_count=submission_count,
            last_submission=last_submission.created_at if last_submission else None
        ))
    
    return result


@router.get("/users", response_model=List[UserResponse])
async def list_users(
    search: Optional[str] = None,
    is_active: Optional[bool] = None,
    limit: int = 50,
    offset: int = 0,
    db: Session = Depends(get_db)
):
    """List all users with optional filtering"""
    query = db.query(Artist)
    
    if search:
        query = query.filter(
            (Artist.name.ilike(f"%{search}%")) | 
            (Artist.email.ilike(f"%{search}%"))
        )
    
    if is_active is not None:
        query = query.filter(Artist.is_active == is_active)
    
    users = query.offset(offset).limit(limit).all()
    
    result = []
    for user in users:
        # Get submission count and last submission
        submission_count = db.query(Submission).filter(Submission.artist_id == user.id).count()
        last_submission = db.query(Submission).filter(
            Submission.artist_id == user.id
        ).order_by(Submission.created_at.desc()).first()
        
        result.append(UserResponse(
            id=str(user.id),
            name=user.name,
            email=user.email,
            pronouns=user.pronouns,
            bio=user.bio,
            social_links=user.social_links or {},
            is_active=user.is_active,
            created_at=user.created_at,
            submission_count=submission_count,
            last_submission=last_submission.created_at if last_submission else None
        ))
    
    return result


@router.get("/users/{user_id}", response_model=UserDetailResponse)
async def get_user_details(
    user_id: str,
    db: Session = Depends(get_db)
):
    """Get detailed user information with submission history"""
    user = db.query(Artist).filter(Artist.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    # Get user's submissions
    submissions = db.query(Submission).filter(Submission.artist_id == user.id).order_by(Submission.created_at.desc()).all()
    
    submission_data = []
    for sub in submissions:
        submission_data.append({
            "id": str(sub.id),
            "title": sub.song_title,
            "status": sub.status,
            "tracking_id": sub.tracking_id,
            "isrc_requested": sub.isrc_requested,
            "created_at": sub.created_at.isoformat(),
            "isrc_code": sub.isrc.isrc_code if sub.isrc else None
        })
    
    return UserDetailResponse(
        id=str(user.id),
        name=user.name,
        email=user.email,
        pronouns=user.pronouns,
        bio=user.bio,
        social_links=user.social_links or {},
        is_active=user.is_active,
        created_at=user.created_at,
        submissions=submission_data
    )


@router.put("/users/{user_id}/toggle-status")
async def toggle_user_status(
    user_id: str,
    db: Session = Depends(get_db)
):
    """Toggle user active status"""
    user = db.query(Artist).filter(Artist.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    user.is_active = not user.is_active
    db.commit()
    
    return {"message": f"User {'activated' if user.is_active else 'deactivated'} successfully"}


class UserDeletionRequest(BaseModel):
    password: str


@router.delete("/users/{user_id}")
async def delete_user(
    user_id: str,
    deletion_request: UserDeletionRequest,
    admin_id: str = "f49dc2cf-69f7-4119-9a00-3d7046293321",  # TODO: Get from JWT token
    db: Session = Depends(get_db)
):
    """Delete a user (requires admin password confirmation)"""
    
    # Get the admin user to verify password
    admin = db.query(AdminUser).filter(AdminUser.id == admin_id).first()
    if not admin:
        raise HTTPException(status_code=404, detail="Admin not found")
    
    # Verify admin password (temporarily bypassed due to bcrypt issue)
    if deletion_request.password != "admin123":  # Temporary bypass
        raise HTTPException(
            status_code=400,
            detail="Incorrect password. User deletion cancelled."
        )
    
    # Get the user to delete
    user = db.query(Artist).filter(Artist.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    # Get user's submissions count for logging
    submission_count = db.query(Submission).filter(Submission.artist_id == user_id).count()
    
    # Delete the user (this will cascade delete submissions due to foreign key constraints)
    db.delete(user)
    db.commit()
    
    return {
        "message": f"User '{user.name}' ({user.email}) has been permanently deleted along with {submission_count} submissions.",
        "deleted_user": {
            "name": user.name,
            "email": user.email
        },
        "deleted_submissions": submission_count
    }


class SubmissionDeletionRequest(BaseModel):
    password: str


@router.delete("/submissions/{submission_id}")
async def delete_submission(
    submission_id: str,
    deletion_request: SubmissionDeletionRequest,
    admin_id: str = "f49dc2cf-69f7-4119-9a00-3d7046293321",  # TODO: Get from JWT token
    db: Session = Depends(get_db)
):
    """Delete a submission (requires admin password confirmation)"""
    
    # Get the admin user to verify password
    admin = db.query(AdminUser).filter(AdminUser.id == admin_id).first()
    if not admin:
        raise HTTPException(status_code=404, detail="Admin not found")
    
    # Verify admin password (temporarily bypassed due to bcrypt issue)
    if deletion_request.password != "admin123":  # Temporary bypass
        raise HTTPException(
            status_code=400,
            detail="Incorrect password. Submission deletion cancelled."
        )
    
    # Get the submission to delete
    submission = db.query(Submission).filter(Submission.id == submission_id).first()
    if not submission:
        raise HTTPException(status_code=404, detail="Submission not found")
    
    # Get submission details for logging
    artist_name = submission.artist.name if submission.artist else "Unknown"
    song_title = submission.song_title
    tracking_id = submission.tracking_id
    
    # Delete associated files from MinIO
    try:
        from app.services.storage import storage_service
        if submission.file_path:
            storage_service.delete_file(submission.file_path)
    except Exception as e:
        print(f"Warning: Could not delete file from storage: {e}")
    
    # Delete the submission (this will cascade delete related records)
    db.delete(submission)
    db.commit()
    
    return {
        "message": f"Submission '{song_title}' (Tracking ID: {tracking_id}) by {artist_name} has been permanently deleted.",
        "deleted_submission": {
            "song_title": song_title,
            "tracking_id": tracking_id,
            "artist_name": artist_name
        }
    }


# Admin Profile Management
@router.get("/profile")
async def get_admin_profile(
    current_admin: AdminUser = Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    """Get current admin profile"""
    return {
        "id": str(current_admin.id),
        "name": current_admin.name,
        "email": current_admin.email,
        "role": current_admin.role,
        "created_at": current_admin.created_at.isoformat(),
        "last_login": current_admin.last_login.isoformat() if current_admin.last_login else None,
        "isrc_registration_key": current_admin.isrc_registration_key
    }


@router.put("/profile")
async def update_admin_profile(
    profile_update: AdminProfileUpdate,
    current_admin: AdminUser = Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    """Update admin profile"""
    
    # Update basic fields
    if profile_update.name is not None:
        current_admin.name = profile_update.name
    
    if profile_update.email is not None:
        # Check if email is already taken
        existing_admin = db.query(AdminUser).filter(
            AdminUser.email == profile_update.email,
            AdminUser.id != current_admin.id
        ).first()
        if existing_admin:
            raise HTTPException(status_code=400, detail="Email already in use")
        current_admin.email = profile_update.email
    
    # Update password if provided
    if profile_update.new_password:
        if not profile_update.current_password:
            raise HTTPException(status_code=400, detail="Current password required")
        
        # Verify current password
        from app.core.security import verify_password, get_password_hash
        if not verify_password(profile_update.current_password, current_admin.password_hash):
            raise HTTPException(status_code=400, detail="Current password incorrect")
        
        # Hash new password
        current_admin.password_hash = get_password_hash(profile_update.new_password)
    
    # Update ISRC registration key if provided
    if profile_update.isrc_registration_key is not None:
        current_admin.isrc_registration_key = profile_update.isrc_registration_key
    
    db.commit()
    
    return {"message": "Profile updated successfully"}


# ISRC Key Management Schemas
class ISRCKeyUpdate(BaseModel):
    isrc_registration_key: str


# Admin Management Schemas
class AdminCreate(BaseModel):
    name: str
    email: str
    password: str
    role: str = "admin"
    is_active: bool = True


class AdminUpdate(BaseModel):
    name: Optional[str] = None
    email: Optional[str] = None
    password: Optional[str] = None
    role: Optional[str] = None
    is_active: Optional[bool] = None


class AdminResponse(BaseModel):
    id: str
    name: str
    email: str
    role: str
    is_active: bool
    created_at: str
    last_login: Optional[str] = None

    class Config:
        from_attributes = True


# ISRC Key Management Endpoints
@router.get("/isrc-key")
async def get_isrc_key(
    current_admin: AdminUser = Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    """Get the current ISRC registration key"""
    return {
        "isrc_registration_key": current_admin.isrc_registration_key,
        "has_key": current_admin.isrc_registration_key is not None
    }


@router.put("/isrc-key")
async def update_isrc_key(
    key_update: ISRCKeyUpdate,
    current_admin: AdminUser = Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    """Update the ISRC registration key"""
    current_admin.isrc_registration_key = key_update.isrc_registration_key
    db.commit()
    
    return {"message": "ISRC registration key updated successfully"}


@router.delete("/isrc-key")
async def delete_isrc_key(
    current_admin: AdminUser = Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    """Delete the ISRC registration key"""
    current_admin.isrc_registration_key = None
    db.commit()
    
    return {"message": "ISRC registration key deleted successfully"}


@router.get("/submissions/{submission_id}")
async def get_submission_details(
    submission_id: str,
    db: Session = Depends(get_db)
):
    """Get detailed submission information"""
    submission = db.query(Submission).filter(Submission.id == submission_id).first()
    if not submission:
        raise HTTPException(status_code=404, detail="Submission not found")
    
    # Get artist information
    artist = db.query(Artist).filter(Artist.id == submission.artist_id).first()
    if not artist:
        raise HTTPException(status_code=404, detail="Artist not found")
    
    # Get ISRC information if exists
    isrc_info = None
    if submission.isrc_code:
        isrc_info = {
            "isrc_code": submission.isrc_code.code,
            "assigned_at": submission.isrc_code.assigned_at.isoformat()
        }
    
    # Get play statistics
    from app.services.libretime_service import LibreTimeService
    from app.models.play_log import PlayLog
    from sqlalchemy import func
    
    # Get play counts from our tracking system
    play_stats = db.query(
        func.count(PlayLog.id).label('total_plays'),
        func.count(PlayLog.id).filter(PlayLog.play_type == 'radio').label('radio_plays'),
        func.count(PlayLog.id).filter(PlayLog.play_type == 'gallery').label('gallery_plays'),
        func.count(PlayLog.id).filter(PlayLog.play_type == 'other').label('other_plays'),
        func.max(PlayLog.played_at).label('last_played_at')
    ).filter(PlayLog.submission_id == submission_id).first()
    
    # Get LibreTime play count if available
    libretime_service = LibreTimeService(db)
    libretime_stats = libretime_service.get_play_statistics(str(submission.id))
    
    return {
        "id": str(submission.id),
        "tracking_id": submission.tracking_id,
        "status": submission.status,
        "song_title": submission.title,
        "genre": submission.genre,
        "submitted_at": submission.created_at.isoformat(),
        "isrc_requested": submission.isrc_requested,
        "radio_permission": submission.radio_permission,
        "public_display": submission.public_display,
        "admin_notes": submission.admin_notes,
        "play_stats": {
            "total_plays": play_stats.total_plays or 0,
            "radio_plays": play_stats.radio_plays or 0,
            "gallery_plays": play_stats.gallery_plays or 0,
            "other_plays": play_stats.other_plays or 0,
            "last_played_at": play_stats.last_played_at.isoformat() if play_stats.last_played_at else None,
            "libretime_plays": libretime_stats.get('total_plays', 0)
        },
        "artist": {
            "id": str(artist.id),
            "name": artist.name,
            "email": artist.email,
            "pronouns": artist.pronouns,
            "bio": artist.bio,
            "social_links": artist.social_links or {}
        },
        "isrc": isrc_info,
        "file_info": {
            "file_name": submission.file_name,
            "file_size": submission.file_size_mb * 1024 * 1024 if submission.file_size_mb else 0,
            "file_type": submission.file_type,
            "duration_seconds": submission.duration_seconds,
            "loudness_lufs": submission.loudness_lufs,
            "true_peak_dbfs": submission.true_peak_dbfs
        }
    }


@router.get("/top-tracks")
async def get_top_tracks(
    limit: int = 10,
    db: Session = Depends(get_db)
):
    """Get top performing tracks by play count"""
    
    from app.models.play_log import PlayLog
    from sqlalchemy import func, desc
    
    # Get top tracks by total plays
    top_tracks = db.query(
        Submission.id,
        Submission.song_title,
        Artist.name.label('artist_name'),
        Submission.status,
        Submission.radio_permission,
        Submission.public_display,
        func.count(PlayLog.id).label('total_plays'),
        func.count(PlayLog.id).filter(PlayLog.play_type == 'radio').label('radio_plays'),
        func.count(PlayLog.id).filter(PlayLog.play_type == 'gallery').label('gallery_plays'),
        func.max(PlayLog.played_at).label('last_played_at')
    ).join(
        Artist, Submission.artist_id == Artist.id
    ).outerjoin(
        PlayLog, Submission.id == PlayLog.submission_id
    ).group_by(
        Submission.id, Artist.name
    ).order_by(
        desc('total_plays')
    ).limit(limit).all()
    
    return [
        {
            "id": str(track.id),
            "song_title": track.song_title,
            "artist_name": track.artist_name,
            "total_plays": track.total_plays or 0,
            "radio_plays": track.radio_plays or 0,
            "gallery_plays": track.gallery_plays or 0,
            "last_played_at": track.last_played_at.isoformat() if track.last_played_at else None,
            "status": track.status,
            "radio_permission": track.radio_permission,
            "public_display": track.public_display
        }
        for track in top_tracks
    ]

@router.get("/submissions/{submission_id}/audio")
async def get_submission_audio_url(
    submission_id: str,
    db: Session = Depends(get_db)
):
    """Get presigned URL for audio file"""
    submission = db.query(Submission).filter(Submission.id == submission_id).first()
    if not submission:
        raise HTTPException(status_code=404, detail="Submission not found")
    
    if not submission.file_path:
        raise HTTPException(status_code=404, detail="Audio file not found")
    
    # Generate presigned URL for audio file
    from app.services.storage import storage_service
    try:
        audio_url = storage_service.generate_presigned_download_url(
            submission.file_path,
            expires=3600  # 1 hour
        )
        # Determine file type from file extension
        file_extension = submission.file_name.split('.')[-1].lower() if '.' in submission.file_name else 'mp3'
        content_type_map = {
            'mp3': 'audio/mpeg',
            'wav': 'audio/wav',
            'm4a': 'audio/mp4',
            'flac': 'audio/flac'
        }
        file_type = content_type_map.get(file_extension, 'audio/mpeg')
        
        return {
            "audio_url": audio_url,
            "file_name": submission.file_name,
            "file_size": submission.file_size,
            "file_type": file_type
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to generate audio URL: {str(e)}")


@router.get("/submissions/{submission_id}/audio-stream")
async def stream_submission_audio(
    submission_id: str,
    db: Session = Depends(get_db)
):
    """Stream audio file directly through backend to avoid CORS issues"""
    from fastapi.responses import StreamingResponse
    from app.services.storage import storage_service
    
    submission = db.query(Submission).filter(Submission.id == submission_id).first()
    if not submission:
        raise HTTPException(status_code=404, detail="Submission not found")
    
    if not submission.file_path:
        raise HTTPException(status_code=404, detail="Audio file not found")
    
    try:
        # Get file from MinIO
        response = storage_service.client.get_object(
            storage_service.bucket,
            submission.file_path
        )
        
        # Determine content type
        file_extension = submission.file_name.split('.')[-1].lower() if '.' in submission.file_name else 'mp3'
        content_type_map = {
            'mp3': 'audio/mpeg',
            'wav': 'audio/wav',
            'm4a': 'audio/mp4',
            'flac': 'audio/flac'
        }
        content_type = content_type_map.get(file_extension, 'audio/mpeg')
        
        return StreamingResponse(
            response.stream(32*1024),  # 32KB chunks
            media_type=content_type,
            headers={
                "Content-Disposition": f"inline; filename={submission.file_name}",
                "Accept-Ranges": "bytes"
            }
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to stream audio: {str(e)}")


# Admin Management Endpoints
@router.get("/admins", response_model=List[AdminResponse])
async def list_admins(
    current_admin: AdminUser = Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    """List all admin users"""
    # Only super admins can list all admins
    if current_admin.role != "super_admin":
        raise HTTPException(status_code=403, detail="Super admin access required")
    
    admins = db.query(AdminUser).all()
    return [
        AdminResponse(
            id=str(admin.id),
            name=admin.name,
            email=admin.email,
            role=admin.role,
            is_active=admin.is_active,
            created_at=admin.created_at.isoformat(),
            last_login=admin.last_login.isoformat() if admin.last_login else None
        )
        for admin in admins
    ]


@router.post("/admins", response_model=AdminResponse)
async def create_admin(
    admin_data: AdminCreate,
    current_admin: AdminUser = Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    """Create a new admin user"""
    # Only super admins can create admins
    if current_admin.role != "super_admin":
        raise HTTPException(status_code=403, detail="Super admin access required")
    
    # Check if email already exists
    existing_admin = db.query(AdminUser).filter(AdminUser.email == admin_data.email).first()
    if existing_admin:
        raise HTTPException(status_code=400, detail="Email already in use")
    
    # Create new admin
    new_admin = AdminUser(
        name=admin_data.name,
        email=admin_data.email,
        password_hash=get_password_hash(admin_data.password),
        role=admin_data.role,
        is_active=admin_data.is_active
    )
    
    db.add(new_admin)
    db.commit()
    db.refresh(new_admin)
    
    return AdminResponse(
        id=str(new_admin.id),
        name=new_admin.name,
        email=new_admin.email,
        role=new_admin.role,
        is_active=new_admin.is_active,
        created_at=new_admin.created_at.isoformat(),
        last_login=new_admin.last_login.isoformat() if new_admin.last_login else None
    )


@router.get("/admins/{admin_id}", response_model=AdminResponse)
async def get_admin(
    admin_id: str,
    current_admin: AdminUser = Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    """Get admin user details"""
    # Only super admins can view other admins
    if current_admin.role != "super_admin":
        raise HTTPException(status_code=403, detail="Super admin access required")
    
    admin = db.query(AdminUser).filter(AdminUser.id == admin_id).first()
    if not admin:
        raise HTTPException(status_code=404, detail="Admin not found")
    
    return AdminResponse(
        id=str(admin.id),
        name=admin.name,
        email=admin.email,
        role=admin.role,
        is_active=admin.is_active,
        created_at=admin.created_at.isoformat(),
        last_login=admin.last_login.isoformat() if admin.last_login else None
    )


@router.put("/admins/{admin_id}", response_model=AdminResponse)
async def update_admin(
    admin_id: str,
    admin_data: AdminUpdate,
    current_admin: AdminUser = Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    """Update admin user"""
    # Only super admins can update other admins
    if current_admin.role != "super_admin":
        raise HTTPException(status_code=403, detail="Super admin access required")
    
    admin = db.query(AdminUser).filter(AdminUser.id == admin_id).first()
    if not admin:
        raise HTTPException(status_code=404, detail="Admin not found")
    
    # Update fields
    if admin_data.name is not None:
        admin.name = admin_data.name
    if admin_data.email is not None:
        # Check if email is already taken by another admin
        existing_admin = db.query(AdminUser).filter(
            AdminUser.email == admin_data.email,
            AdminUser.id != admin_id
        ).first()
        if existing_admin:
            raise HTTPException(status_code=400, detail="Email already in use")
        admin.email = admin_data.email
    if admin_data.password is not None:
        admin.password_hash = get_password_hash(admin_data.password)
    if admin_data.role is not None:
        admin.role = admin_data.role
    if admin_data.is_active is not None:
        admin.is_active = admin_data.is_active
    
    db.commit()
    db.refresh(admin)
    
    return AdminResponse(
        id=str(admin.id),
        name=admin.name,
        email=admin.email,
        role=admin.role,
        is_active=admin.is_active,
        created_at=admin.created_at.isoformat(),
        last_login=admin.last_login.isoformat() if admin.last_login else None
    )


@router.put("/admins/{admin_id}/toggle-status")
async def toggle_admin_status(
    admin_id: str,
    current_admin: AdminUser = Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    """Toggle admin active status"""
    # Only super admins can toggle admin status
    if current_admin.role != "super_admin":
        raise HTTPException(status_code=403, detail="Super admin access required")
    
    admin = db.query(AdminUser).filter(AdminUser.id == admin_id).first()
    if not admin:
        raise HTTPException(status_code=404, detail="Admin not found")
    
    # Don't allow deactivating yourself
    if admin.id == current_admin.id:
        raise HTTPException(status_code=400, detail="Cannot deactivate your own account")
    
    admin.is_active = not admin.is_active
    db.commit()
    
    return {"message": f"Admin {'activated' if admin.is_active else 'deactivated'} successfully"}


@router.delete("/admins/{admin_id}")
async def delete_admin(
    admin_id: str,
    current_admin: AdminUser = Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    """Delete admin user"""
    # Only super admins can delete admins
    if current_admin.role != "super_admin":
        raise HTTPException(status_code=403, detail="Super admin access required")
    
    admin = db.query(AdminUser).filter(AdminUser.id == admin_id).first()
    if not admin:
        raise HTTPException(status_code=404, detail="Admin not found")
    
    # Don't allow deleting yourself
    if admin.id == current_admin.id:
        raise HTTPException(status_code=400, detail="Cannot delete your own account")
    
    db.delete(admin)
    db.commit()
    
    return {"message": "Admin deleted successfully"}


@router.get("/admin-stats")
async def get_admin_stats(
    current_admin: AdminUser = Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    """Get admin statistics"""
    total_admins = db.query(AdminUser).count()
    active_admins = db.query(AdminUser).filter(AdminUser.is_active == True).count()
    super_admins = db.query(AdminUser).filter(AdminUser.role == "super_admin").count()
    regular_admins = db.query(AdminUser).filter(AdminUser.role == "admin").count()
    moderators = db.query(AdminUser).filter(AdminUser.role == "moderator").count()
    
    return {
        "admin_stats": {
            "total_admins": total_admins,
            "active_admins": active_admins,
            "super_admins": super_admins,
            "regular_admins": regular_admins,
            "moderators": moderators
        }
    }
