from sqlalchemy.orm import Session
from app.models.admin_notification import AdminNotification, CommercialUseLog
from app.models.rights_permission import RightsPermission, RightsPermissionHistory
from app.models.submission import Submission
from app.models.artist import Artist
from app.services.email import email_service
from typing import Optional
from datetime import datetime
import uuid


class NotificationService:
    def __init__(self, db: Session):
        self.db = db

    def create_rights_change_notification(
        self, 
        submission_id: str, 
        permission_type: str, 
        action: str, 
        previous_value: bool, 
        new_value: bool,
        changed_by_artist_id: Optional[str] = None,
        changed_by_admin_id: Optional[str] = None,
        reason: Optional[str] = None
    ):
        """Create notification when rights permissions change"""
        
        # Get submission and artist info
        submission = self.db.query(Submission).filter(Submission.id == submission_id).first()
        if not submission:
            return None
            
        artist = self.db.query(Artist).filter(Artist.id == submission.artist_id).first()
        if not artist:
            return None
        
        # Determine priority based on action and permission type
        priority = "medium"
        if action == "revoked" and permission_type in ["radio_play", "commercial_use"]:
            priority = "high"
        elif action == "revoked" and permission_type == "podcast":
            priority = "medium"
        elif action == "granted" and permission_type == "commercial_use":
            priority = "high"
        
        # Create notification
        notification = AdminNotification(
            notification_type="rights_change",
            title=f"Rights Permission {action.title()}: {permission_type.replace('_', ' ').title()}",
            message=f"Artist {artist.name} ({artist.email}) {action} {permission_type.replace('_', ' ')} permission for '{submission.song_title}'. {f'Reason: {reason}' if reason else ''}",
            priority=priority,
            submission_id=submission_id,
            artist_id=artist.id,
            extra_data={
                "permission_type": permission_type,
                "action": action,
                "previous_value": previous_value,
                "new_value": new_value,
                "changed_by_artist_id": changed_by_artist_id,
                "changed_by_admin_id": changed_by_admin_id,
                "reason": reason
            }
        )
        
        self.db.add(notification)
        self.db.commit()
        
        # Send email notification to admin
        self._send_rights_change_email(artist, submission, permission_type, action, reason)
        
        return notification

    def create_commercial_use_notification(
        self,
        submission_id: str,
        use_type: str,
        use_description: str,
        compensation_amount: str,
        admin_id: str
    ):
        """Create notification when music is used commercially"""
        
        submission = self.db.query(Submission).filter(Submission.id == submission_id).first()
        if not submission:
            return None
            
        artist = self.db.query(Artist).filter(Artist.id == submission.artist_id).first()
        if not artist:
            return None
        
        # Create commercial use log
        commercial_log = CommercialUseLog(
            submission_id=submission_id,
            rights_permission_id=submission.rights_permission.id if submission.rights_permission else None,
            use_type=use_type,
            use_description=use_description,
            use_date=datetime.utcnow(),
            compensation_rate="per_use",  # Default to per use
            compensation_amount=compensation_amount,
            created_by_admin_id=admin_id
        )
        
        self.db.add(commercial_log)
        
        # Create notification
        notification = AdminNotification(
            notification_type="commercial_use",
            title=f"Commercial Use: {use_type.title()}",
            message=f"Music by {artist.name} used for {use_type}: {use_description}. Compensation: {compensation_amount}",
            priority="high",
            submission_id=submission_id,
            artist_id=artist.id,
            extra_data={
                "use_type": use_type,
                "use_description": use_description,
                "compensation_amount": compensation_amount,
                "admin_id": admin_id
            }
        )
        
        self.db.add(notification)
        self.db.commit()
        
        # Send email to admin
        self._send_commercial_use_email(artist, submission, use_type, use_description, compensation_amount)
        
        return notification

    def create_compensation_due_notification(
        self,
        submission_id: str,
        amount_due: float,
        reason: str
    ):
        """Create notification when compensation is due to artist"""
        
        submission = self.db.query(Submission).filter(Submission.id == submission_id).first()
        if not submission:
            return None
            
        artist = self.db.query(Artist).filter(Artist.id == submission.artist_id).first()
        if not artist:
            return None
        
        notification = AdminNotification(
            notification_type="compensation_due",
            title=f"Compensation Due: ${amount_due:.2f}",
            message=f"Artist {artist.name} is owed ${amount_due:.2f} for commercial use of '{submission.song_title}'. {reason}",
            priority="urgent",
            submission_id=submission_id,
            artist_id=artist.id,
            extra_data={
                "amount_due": amount_due,
                "reason": reason
            }
        )
        
        self.db.add(notification)
        self.db.commit()
        
        # Send email to admin
        self._send_compensation_due_email(artist, submission, amount_due, reason)
        
        return notification

    def _send_rights_change_email(self, artist, submission, permission_type, action, reason):
        """Send email notification about rights changes"""
        subject = f"Rights Permission {action.title()}: {submission.song_title}"
        
        html_content = f"""
        <html>
        <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
            <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; text-align: center;">
                <h1>🎵 GayPHX Music - Rights Change Alert</h1>
            </div>
            <div style="padding: 20px;">
                <h2>Rights Permission {action.title()}</h2>
                <p><strong>Artist:</strong> {artist.name} ({artist.email})</p>
                <p><strong>Song:</strong> {submission.song_title}</p>
                <p><strong>Permission:</strong> {permission_type.replace('_', ' ').title()}</p>
                <p><strong>Action:</strong> {action.title()}</p>
                {f'<p><strong>Reason:</strong> {reason}</p>' if reason else ''}
                
                <div style="background: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <p><strong>Action Required:</strong> Please review this change and ensure all systems are updated accordingly.</p>
                </div>
                
                <p>This notification was automatically generated by the GayPHX Music platform.</p>
            </div>
        </body>
        </html>
        """
        
        # Send to admin email (you'll need to configure this)
        admin_email = "admin@gayphx.com"  # Configure this
        email_service.send_email(admin_email, subject, html_content)

    def _send_commercial_use_email(self, artist, submission, use_type, use_description, compensation_amount):
        """Send email notification about commercial use"""
        subject = f"Commercial Use: {submission.song_title}"
        
        html_content = f"""
        <html>
        <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
            <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; text-align: center;">
                <h1>🎵 GayPHX Music - Commercial Use Alert</h1>
            </div>
            <div style="padding: 20px;">
                <h2>Music Used Commercially</h2>
                <p><strong>Artist:</strong> {artist.name} ({artist.email})</p>
                <p><strong>Song:</strong> {submission.song_title}</p>
                <p><strong>Use Type:</strong> {use_type}</p>
                <p><strong>Description:</strong> {use_description}</p>
                <p><strong>Compensation:</strong> {compensation_amount}</p>
                
                <div style="background: #fff3cd; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #ffc107;">
                    <p><strong>Important:</strong> Ensure compensation is processed and tracked properly.</p>
                </div>
                
                <p>This notification was automatically generated by the GayPHX Music platform.</p>
            </div>
        </body>
        </html>
        """
        
        admin_email = "admin@gayphx.com"  # Configure this
        email_service.send_email(admin_email, subject, html_content)

    def _send_compensation_due_email(self, artist, submission, amount_due, reason):
        """Send email notification about compensation due"""
        subject = f"Compensation Due: ${amount_due:.2f} - {submission.song_title}"
        
        html_content = f"""
        <html>
        <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
            <div style="background: linear-gradient(135deg, #dc3545 0%, #c82333 100%); color: white; padding: 20px; text-align: center;">
                <h1>💰 GayPHX Music - Compensation Due</h1>
            </div>
            <div style="padding: 20px;">
                <h2>Payment Required</h2>
                <p><strong>Artist:</strong> {artist.name} ({artist.email})</p>
                <p><strong>Song:</strong> {submission.song_title}</p>
                <p><strong>Amount Due:</strong> ${amount_due:.2f}</p>
                <p><strong>Reason:</strong> {reason}</p>
                
                <div style="background: #f8d7da; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #dc3545;">
                    <p><strong>Urgent:</strong> Please process this payment as soon as possible to maintain good relationships with our artists.</p>
                </div>
                
                <p>This notification was automatically generated by the GayPHX Music platform.</p>
            </div>
        </body>
        </html>
        """
        
        admin_email = "admin@gayphx.com"  # Configure this
        email_service.send_email(admin_email, subject, html_content)

    def get_unread_notifications(self, limit: int = 50):
        """Get unread notifications for admin dashboard"""
        return self.db.query(AdminNotification).filter(
            AdminNotification.is_read == False
        ).order_by(AdminNotification.created_at.desc()).limit(limit).all()

    def mark_notification_read(self, notification_id: str):
        """Mark a notification as read"""
        notification = self.db.query(AdminNotification).filter(
            AdminNotification.id == notification_id
        ).first()
        
        if notification:
            notification.is_read = True
            notification.read_at = datetime.utcnow()
            self.db.commit()
            
        return notification

    def mark_notification_resolved(self, notification_id: str):
        """Mark a notification as resolved"""
        notification = self.db.query(AdminNotification).filter(
            AdminNotification.id == notification_id
        ).first()
        
        if notification:
            notification.is_resolved = True
            notification.resolved_at = datetime.utcnow()
            self.db.commit()
            
        return notification
