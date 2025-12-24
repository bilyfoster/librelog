"""
Notification service for email and in-app notifications
"""

import smtplib
from uuid import UUID
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from typing import List, Dict, Any, Optional
from datetime import datetime
import structlog
import os

from backend.models.notification import Notification, NotificationType, NotificationStatus
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, update

logger = structlog.get_logger()


class NotificationService:
    """Service for managing notifications"""

    @staticmethod
    async def create_notification(
        db: AsyncSession,
        user_id: UUID,
        message: str,
        notification_type: NotificationType = NotificationType.IN_APP,
        subject: Optional[str] = None,
        metadata: Optional[Dict[str, Any]] = None
    ) -> Notification:
        """
        Create a new notification
        
        Args:
            db: Database session
            user_id: User ID to notify
            message: Notification message
            notification_type: Type of notification
            subject: Email subject (for email notifications)
            metadata: Additional metadata
            
        Returns:
            Created notification
        """
        notification = Notification(
            user_id=user_id,
            notification_type=notification_type,
            subject=subject,
            message=message,
            meta_data=metadata or {},
            status=NotificationStatus.PENDING
        )
        db.add(notification)
        await db.commit()
        await db.refresh(notification)

        # Send email if needed
        if notification_type in [NotificationType.EMAIL, NotificationType.BOTH]:
            await NotificationService._send_email(notification, db)

        return notification

    @staticmethod
    async def send_notification(
        db: AsyncSession,
        notification: Notification
    ) -> bool:
        """
        Send a pending notification
        
        Args:
            db: Database session
            notification: Notification to send
            
        Returns:
            True if successful
        """
        if notification.status != NotificationStatus.PENDING:
            return False

        try:
            if notification.notification_type in [NotificationType.EMAIL, NotificationType.BOTH]:
                success = await NotificationService._send_email(notification, db)
                if not success:
                    return False

            # Mark as sent
            await db.execute(
                update(Notification)
                .where(Notification.id == notification.id)
                .values(
                    status=NotificationStatus.SENT,
                    sent_at=datetime.utcnow()
                )
            )
            await db.commit()

            logger.info("notification_sent", notification_id=notification.id)
            return True

        except Exception as e:
            logger.error(
                "notification_failed",
                notification_id=notification.id,
                error=str(e),
                exc_info=True
            )

            await db.execute(
                update(Notification)
                .where(Notification.id == notification.id)
                .values(
                    status=NotificationStatus.FAILED,
                    error_message=str(e)
                )
            )
            await db.commit()
            return False

    @staticmethod
    async def _send_email(notification: Notification, db: AsyncSession = None) -> bool:
        """Send email notification"""
        smtp_host = os.getenv("SMTP_HOST", "smtp.gmail.com")  # Default to Gmail SMTP instead of localhost
        smtp_port = int(os.getenv("SMTP_PORT", "587"))
        smtp_user = os.getenv("SMTP_USER")
        smtp_password = os.getenv("SMTP_PASSWORD")
        smtp_from = os.getenv("SMTP_FROM", "noreply@librelog.local")

        if not smtp_user or not smtp_password:
            logger.warning("smtp_not_configured", notification_id=notification.id)
            return False

        try:
            # Get user email from metadata or fetch from database
            user_email = None
            if notification.meta_data:
                user_email = notification.meta_data.get("user_email")
            
            # If not in metadata and we have a db session, fetch from User model
            if not user_email and db:
                from backend.models.user import User
                from sqlalchemy import select
                result = await db.execute(
                    select(User).where(User.id == notification.user_id)
                )
                user = result.scalar_one_or_none()
                if user and hasattr(user, 'email') and user.email:
                    user_email = user.email
            
            if not user_email:
                logger.warning("no_user_email", notification_id=notification.id)
                return False

            msg = MIMEMultipart()
            msg['From'] = smtp_from
            msg['To'] = user_email
            msg['Subject'] = notification.subject or "LibreLog Notification"
            msg.attach(MIMEText(notification.message, 'plain'))

            with smtplib.SMTP(smtp_host, smtp_port) as server:
                server.starttls()
                server.login(smtp_user, smtp_password)
                server.send_message(msg)

            logger.info("email_sent", notification_id=notification.id, to=user_email)
            return True

        except Exception as e:
            logger.error(
                "email_failed",
                notification_id=notification.id,
                error=str(e),
                exc_info=True
            )
            return False

    @staticmethod
    async def mark_read(
        db: AsyncSession,
        notification_id: UUID,
        user_id: UUID
    ) -> bool:
        """Mark notification as read"""
        result = await db.execute(
            select(Notification).where(
                Notification.id == notification_id,
                Notification.user_id == user_id
            )
        )
        notification = result.scalar_one_or_none()

        if not notification:
            return False

        await db.execute(
            update(Notification)
            .where(Notification.id == notification_id)
            .values(
                status=NotificationStatus.READ,
                read_at=datetime.utcnow()
            )
        )
        await db.commit()
        return True

    @staticmethod
    async def get_user_notifications(
        db: AsyncSession,
        user_id: UUID,
        unread_only: bool = False,
        limit: int = 50
    ) -> List[Notification]:
        """Get notifications for a user"""
        query = select(Notification).where(Notification.user_id == user_id)

        if unread_only:
            query = query.where(Notification.status != NotificationStatus.READ)

        query = query.order_by(Notification.created_at.desc()).limit(limit)
        result = await db.execute(query)
        return list(result.scalars().all())

