"""
Notifications router for email and in-app notifications
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, update
from typing import List, Optional
from pydantic import BaseModel, Field

from backend.database import get_db
from backend.models.notification import Notification, NotificationType, NotificationStatus
from backend.models.user import User
from backend.routers.auth import get_current_user
from backend.models.user_roles import has_permission, Permission, Role
from backend.services.notification_service import NotificationService

router = APIRouter(prefix="/notifications", tags=["notifications"])


class NotificationResponse(BaseModel):
    id: UUID
    notification_type: NotificationType
    status: NotificationStatus
    subject: Optional[str] = None
    message: str
    metadata: Optional[dict] = Field(None, alias="meta_data")
    read_at: Optional[str] = None
    sent_at: Optional[str] = None
    created_at: str

    class Config:
        from_attributes = True
        populate_by_name = True  # Allow both meta_data and metadata


class NotificationCreate(BaseModel):
    user_id: UUID
    message: str
    notification_type: NotificationType = NotificationType.IN_APP
    subject: Optional[str] = None
    metadata: Optional[dict] = None


@router.get("/", response_model=List[NotificationResponse])
async def list_notifications(
    unread_only: bool = Query(False),
    skip: int = Query(0, ge=0),
    limit: int = Query(50, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get notifications for current user"""
    notifications = await NotificationService.get_user_notifications(
        db,
        current_user.id,
        unread_only=unread_only,
        limit=limit
    )
    return notifications


@router.get("/unread-count", response_model=dict)
async def get_unread_count(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get count of unread notifications"""
    notifications = await NotificationService.get_user_notifications(
        db,
        current_user.id,
        unread_only=True,
        limit=1000
    )
    return {"count": len(notifications)}


@router.post("/", response_model=NotificationResponse, status_code=status.HTTP_201_CREATED)
async def create_notification(
    notification: NotificationCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new notification (admin only)"""
    # Check if user has permission to create notifications for others
    if notification.user_id != current_user.id:
        # Only admins can create notifications for other users
        if not has_permission(current_user, Permission.CREATE_USERS):
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Only admins can create notifications for other users"
            )

    new_notification = await NotificationService.create_notification(
        db,
        notification.user_id,
        notification.message,
        notification.notification_type,
        notification.subject,
        notification.metadata
    )
    return new_notification


@router.post("/{notification_id}/read", status_code=status.HTTP_200_OK)
@router.put("/{notification_id}/read", status_code=status.HTTP_200_OK)
async def mark_notification_read(
    notification_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Mark a notification as read (supports both POST and PUT)"""
    success = await NotificationService.mark_read(db, notification_id, current_user.id)
    if not success:
        raise HTTPException(status_code=404, detail="Notification not found")
    return {"status": "success"}


@router.post("/mark-all-read", status_code=status.HTTP_200_OK)
async def mark_all_read(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Mark all notifications as read for current user"""
    notifications = await NotificationService.get_user_notifications(
        db,
        current_user.id,
        unread_only=True,
        limit=1000
    )

    for notification in notifications:
        await NotificationService.mark_read(db, notification.id, current_user.id)

    return {"status": "success", "count": len(notifications)}

