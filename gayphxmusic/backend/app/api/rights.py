from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.models.submission import Submission
from app.models.rights_permission import RightsPermission, RightsPermissionHistory
from app.models.artist import Artist
from app.models.admin_user import AdminUser
from app.models.admin_notification import AdminNotification
from app.services.notification_service import NotificationService
from pydantic import BaseModel
from typing import Optional
from datetime import datetime
import uuid

router = APIRouter()


class RightsPermissionUpdate(BaseModel):
    radio_play_permission: Optional[bool] = None
    public_display_permission: Optional[bool] = None
    podcast_permission: Optional[bool] = None  # Changed from streaming_permission
    commercial_use_permission: Optional[bool] = None
    custom_terms: Optional[str] = None
    restrictions: Optional[str] = None
    reason: Optional[str] = None


class RightsPermissionCreate(BaseModel):
    submission_id: str
    radio_play_permission: bool = False
    public_display_permission: bool = False
    podcast_permission: bool = False  # Changed from streaming_permission
    commercial_use_permission: bool = False
    rights_holder_name: Optional[str] = None
    rights_holder_email: Optional[str] = None
    copyright_year: Optional[str] = None
    copyright_owner: Optional[str] = None
    publisher: Optional[str] = None
    label: Optional[str] = None
    custom_terms: Optional[str] = None
    restrictions: Optional[str] = None
    commercial_compensation_rate: Optional[float] = None


@router.get("/submissions/{submission_id}/rights")
async def get_submission_rights(
    submission_id: str,
    db: Session = Depends(get_db)
):
    """Get rights permissions for a submission"""
    
    submission = db.query(Submission).filter(Submission.id == submission_id).first()
    if not submission:
        raise HTTPException(status_code=404, detail="Submission not found")
    
    rights = db.query(RightsPermission).filter(RightsPermission.submission_id == submission_id).first()
    
    if not rights:
        # Create default rights if none exist
        rights = RightsPermission(
            submission_id=submission_id,
            rights_holder_name=submission.artist.name,
            rights_holder_email=submission.artist.email,
            last_modified_by=submission.artist_id
        )
        db.add(rights)
        db.commit()
        db.refresh(rights)
    
    return {
        "id": str(rights.id),
        "submission_id": str(rights.submission_id),
        "radio_play_permission": rights.radio_play_permission,
        "radio_play_granted_at": rights.radio_play_granted_at.isoformat() if rights.radio_play_granted_at else None,
        "radio_play_revoked_at": rights.radio_play_revoked_at.isoformat() if rights.radio_play_revoked_at else None,
        "public_display_permission": rights.public_display_permission,
        "podcast_permission": rights.podcast_permission,
        "commercial_use_permission": rights.commercial_use_permission,
        "commercial_compensation_rate": float(rights.commercial_compensation_rate) if rights.commercial_compensation_rate else None,
        "rights_holder_name": rights.rights_holder_name,
        "rights_holder_email": rights.rights_holder_email,
        "copyright_year": rights.copyright_year,
        "copyright_owner": rights.copyright_owner,
        "publisher": rights.publisher,
        "label": rights.label,
        "custom_terms": rights.custom_terms,
        "restrictions": rights.restrictions,
        "created_at": rights.created_at.isoformat(),
        "updated_at": rights.updated_at.isoformat()
    }


@router.put("/submissions/{submission_id}/rights")
async def update_submission_rights(
    submission_id: str,
    rights_data: RightsPermissionUpdate,
    artist_id: str = "dfb3f27a-ba8b-4f1b-b036-78d5a79fd796",  # TODO: Get from JWT token
    db: Session = Depends(get_db)
):
    """Update rights permissions for a submission (artist can modify their own rights)"""
    
    submission = db.query(Submission).filter(Submission.id == submission_id).first()
    if not submission:
        raise HTTPException(status_code=404, detail="Submission not found")
    
    # Verify the artist owns this submission
    if str(submission.artist_id) != artist_id:
        raise HTTPException(status_code=403, detail="You can only modify rights for your own submissions")
    
    rights = db.query(RightsPermission).filter(RightsPermission.submission_id == submission_id).first()
    if not rights:
        raise HTTPException(status_code=404, detail="Rights not found for this submission")
    
    # Track changes
    changes_made = []
    now = datetime.utcnow()
    
    # Update radio play permission
    if rights_data.radio_play_permission is not None and rights_data.radio_play_permission != rights.radio_play_permission:
        old_value = rights.radio_play_permission
        rights.radio_play_permission = rights_data.radio_play_permission
        
        if rights_data.radio_play_permission:
            rights.radio_play_granted_at = now
            rights.radio_play_revoked_at = None
        else:
            rights.radio_play_revoked_at = now
        
        changes_made.append({
            "permission_type": "radio_play",
            "action": "granted" if rights_data.radio_play_permission else "revoked",
            "previous_value": old_value,
            "new_value": rights_data.radio_play_permission
        })
    
    # Update other permissions similarly
    if rights_data.public_display_permission is not None and rights_data.public_display_permission != rights.public_display_permission:
        old_value = rights.public_display_permission
        rights.public_display_permission = rights_data.public_display_permission
        
        if rights_data.public_display_permission:
            rights.public_display_granted_at = now
            rights.public_display_revoked_at = None
        else:
            rights.public_display_revoked_at = now
        
        changes_made.append({
            "permission_type": "public_display",
            "action": "granted" if rights_data.public_display_permission else "revoked",
            "previous_value": old_value,
            "new_value": rights_data.public_display_permission
        })
    
    if rights_data.podcast_permission is not None and rights_data.podcast_permission != rights.podcast_permission:
        old_value = rights.podcast_permission
        rights.podcast_permission = rights_data.podcast_permission
        
        if rights_data.podcast_permission:
            rights.podcast_granted_at = now
            rights.podcast_revoked_at = None
        else:
            rights.podcast_revoked_at = now
        
        changes_made.append({
            "permission_type": "podcast",
            "action": "granted" if rights_data.podcast_permission else "revoked",
            "previous_value": old_value,
            "new_value": rights_data.podcast_permission
        })
    
    if rights_data.commercial_use_permission is not None and rights_data.commercial_use_permission != rights.commercial_use_permission:
        old_value = rights.commercial_use_permission
        rights.commercial_use_permission = rights_data.commercial_use_permission
        
        if rights_data.commercial_use_permission:
            rights.commercial_use_granted_at = now
            rights.commercial_use_revoked_at = None
        else:
            rights.commercial_use_revoked_at = now
        
        changes_made.append({
            "permission_type": "commercial_use",
            "action": "granted" if rights_data.commercial_use_permission else "revoked",
            "previous_value": old_value,
            "new_value": rights_data.commercial_use_permission
        })
    
    # Update other fields
    if rights_data.custom_terms is not None:
        rights.custom_terms = rights_data.custom_terms
    
    if rights_data.restrictions is not None:
        rights.restrictions = rights_data.restrictions
    
    rights.last_modified_by = artist_id
    rights.updated_at = now
    
    # Create history records for each change
    for change in changes_made:
        history = RightsPermissionHistory(
            rights_permission_id=rights.id,
            permission_type=change["permission_type"],
            action=change["action"],
            previous_value=change["previous_value"],
            new_value=change["new_value"],
            changed_by_artist_id=artist_id,
            reason=rights_data.reason,
            changed_at=now
        )
        db.add(history)
    
    db.commit()
    db.refresh(rights)
    
    # Send notifications for rights changes
    if changes_made:
        notification_service = NotificationService(db)
        for change in changes_made:
            notification_service.create_rights_change_notification(
                submission_id=submission_id,
                permission_type=change["permission_type"],
                action=change["action"],
                previous_value=change["previous_value"],
                new_value=change["new_value"],
                changed_by_artist_id=artist_id,
                reason=rights_data.reason
            )
    
    return {
        "message": "Rights updated successfully",
        "changes_made": len(changes_made),
        "rights": {
            "radio_play_permission": rights.radio_play_permission,
            "public_display_permission": rights.public_display_permission,
            "podcast_permission": rights.podcast_permission,
            "commercial_use_permission": rights.commercial_use_permission,
            "custom_terms": rights.custom_terms,
            "restrictions": rights.restrictions
        }
    }


@router.get("/submissions/{submission_id}/rights/history")
async def get_rights_history(
    submission_id: str,
    db: Session = Depends(get_db)
):
    """Get rights permission change history for a submission"""
    
    submission = db.query(Submission).filter(Submission.id == submission_id).first()
    if not submission:
        raise HTTPException(status_code=404, detail="Submission not found")
    
    history = db.query(RightsPermissionHistory).filter(
        RightsPermissionHistory.rights_permission_id.in_(
            db.query(RightsPermission.id).filter(RightsPermission.submission_id == submission_id)
        )
    ).order_by(RightsPermissionHistory.changed_at.desc()).all()
    
    history_data = []
    for record in history:
        history_data.append({
            "id": str(record.id),
            "permission_type": record.permission_type,
            "action": record.action,
            "previous_value": record.previous_value,
            "new_value": record.new_value,
            "reason": record.reason,
            "notes": record.notes,
            "changed_at": record.changed_at.isoformat(),
            "changed_by_artist": record.changed_by_artist.name if record.changed_by_artist else None,
            "changed_by_admin": record.changed_by_admin.name if record.changed_by_admin else None
        })
    
    return history_data


@router.get("/admin/radio-permissions")
async def get_radio_permissions(
    active_only: bool = True,
    db: Session = Depends(get_db)
):
    """Get all submissions with radio play permissions (admin view)"""
    
    query = db.query(Submission).join(RightsPermission).join(Artist).filter(
        RightsPermission.radio_play_permission == True
    )
    
    if active_only:
        query = query.filter(RightsPermission.radio_play_revoked_at.is_(None))
    
    submissions = query.all()
    
    result = []
    for submission in submissions:
        rights = submission.rights_permission
        result.append({
            "submission_id": str(submission.id),
            "tracking_id": submission.tracking_id,
            "song_title": submission.song_title,
            "artist_name": submission.artist.name,
            "artist_email": submission.artist.email,
            "radio_permission_granted_at": rights.radio_play_granted_at.isoformat() if rights.radio_play_granted_at else None,
            "radio_permission_revoked_at": rights.radio_play_revoked_at.isoformat() if rights.radio_play_revoked_at else None,
            "is_active": rights.radio_play_revoked_at is None,
            "custom_terms": rights.custom_terms,
            "restrictions": rights.restrictions
        })
    
    return result


class CommercialUseRequest(BaseModel):
    use_type: str  # radio_ad, podcast_ad, event, etc.
    use_description: str
    use_date: datetime
    compensation_amount: str  # "$50", "$100/month", etc.
    notes: Optional[str] = None


@router.post("/submissions/{submission_id}/commercial-use")
async def log_commercial_use(
    submission_id: str,
    commercial_use: CommercialUseRequest,
    admin_id: str = "f49dc2cf-69f7-4119-9a00-3d7046293321",  # TODO: Get from JWT token
    db: Session = Depends(get_db)
):
    """Log commercial use of music (admin only)"""
    
    submission = db.query(Submission).filter(Submission.id == submission_id).first()
    if not submission:
        raise HTTPException(status_code=404, detail="Submission not found")
    
    # Check if commercial use is permitted
    if not submission.rights_permission or not submission.rights_permission.commercial_use_permission:
        raise HTTPException(status_code=400, detail="Commercial use not permitted for this submission")
    
    # Create notification
    notification_service = NotificationService(db)
    notification = notification_service.create_commercial_use_notification(
        submission_id=submission_id,
        use_type=commercial_use.use_type,
        use_description=commercial_use.use_description,
        compensation_amount=commercial_use.compensation_amount,
        admin_id=admin_id
    )
    
    return {
        "message": "Commercial use logged successfully",
        "notification_id": str(notification.id) if notification else None
    }


@router.get("/admin/notifications")
async def get_admin_notifications(
    unread_only: bool = True,
    limit: int = 50,
    db: Session = Depends(get_db)
):
    """Get admin notifications"""
    
    notification_service = NotificationService(db)
    
    if unread_only:
        notifications = notification_service.get_unread_notifications(limit)
    else:
        notifications = db.query(AdminNotification).order_by(
            AdminNotification.created_at.desc()
        ).limit(limit).all()
    
    result = []
    for notification in notifications:
        result.append({
            "id": str(notification.id),
            "type": notification.notification_type,
            "title": notification.title,
            "message": notification.message,
            "priority": notification.priority,
            "is_read": notification.is_read,
            "is_resolved": notification.is_resolved,
            "created_at": notification.created_at.isoformat(),
            "read_at": notification.read_at.isoformat() if notification.read_at else None,
            "resolved_at": notification.resolved_at.isoformat() if notification.resolved_at else None,
            "metadata": notification.extra_data,
            "submission_id": str(notification.submission_id) if notification.submission_id else None,
            "artist_id": str(notification.artist_id) if notification.artist_id else None
        })
    
    return result


@router.put("/admin/notifications/{notification_id}/read")
async def mark_notification_read(
    notification_id: str,
    db: Session = Depends(get_db)
):
    """Mark a notification as read"""
    
    notification_service = NotificationService(db)
    notification = notification_service.mark_notification_read(notification_id)
    
    if not notification:
        raise HTTPException(status_code=404, detail="Notification not found")
    
    return {"message": "Notification marked as read"}


@router.put("/admin/notifications/{notification_id}/resolve")
async def mark_notification_resolved(
    notification_id: str,
    db: Session = Depends(get_db)
):
    """Mark a notification as resolved"""
    
    notification_service = NotificationService(db)
    notification = notification_service.mark_notification_resolved(notification_id)
    
    if not notification:
        raise HTTPException(status_code=404, detail="Notification not found")
    
    return {"message": "Notification marked as resolved"}
