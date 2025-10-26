import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from app.core.config import settings
from typing import Optional
import re
from urllib.parse import urlparse


class EmailService:
    def __init__(self):
        self.smtp_url = settings.smtp_url
        self.frontend_url = settings.frontend_url

    def _parse_smtp_url(self) -> Optional[dict]:
        """Parse SMTP URL into connection parameters"""
        if not self.smtp_url:
            return None
        
        try:
            parsed = urlparse(self.smtp_url)
            return {
                "host": parsed.hostname,
                "port": parsed.port or 587,
                "username": parsed.username,
                "password": parsed.password,
                "use_tls": parsed.scheme == "smtps"
            }
        except Exception:
            return None

    def send_email(self, to_email: str, subject: str, html_content: str, text_content: Optional[str] = None) -> bool:
        """Send email using SMTP"""
        smtp_config = self._parse_smtp_url()
        if not smtp_config:
            print("SMTP not configured, skipping email send")
            return False

        try:
            msg = MIMEMultipart("alternative")
            msg["Subject"] = subject
            msg["From"] = f"GayPHX Music <noreply@gayphx.com>"
            msg["To"] = to_email

            if text_content:
                msg.attach(MIMEText(text_content, "plain"))
            msg.attach(MIMEText(html_content, "html"))

            with smtplib.SMTP(smtp_config["host"], smtp_config["port"]) as server:
                if smtp_config["use_tls"]:
                    server.starttls()
                server.login(smtp_config["username"], smtp_config["password"])
                server.send_message(msg)
            
            return True
        except Exception as e:
            print(f"Error sending email: {e}")
            return False

    def send_submission_confirmation(self, to_email: str, artist_name: str, tracking_id: str) -> bool:
        """Send submission confirmation email"""
        tracking_url = f"{self.frontend_url}/track/{tracking_id}"
        
        subject = "Your Music Submission to GayPHX - Confirmation"
        
        html_content = f"""
        <html>
        <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
            <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; text-align: center;">
                <h1>🎵 GayPHX Music Platform</h1>
            </div>
            <div style="padding: 20px;">
                <h2>Hello {artist_name}!</h2>
                <p>Thank you for submitting your music to GayPHX! We've received your submission and our team will review it soon.</p>
                
                <div style="background: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <h3>Your Submission Details:</h3>
                    <p><strong>Tracking ID:</strong> {tracking_id}</p>
                    <p><strong>Status:</strong> Pending Review</p>
                </div>
                
                <p>You can track your submission status at any time using this link:</p>
                <p><a href="{tracking_url}" style="background: #667eea; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block;">Track Your Submission</a></p>
                
                <p>We'll notify you by email when your submission has been reviewed. If you have any questions, feel free to reach out to us at <a href="mailto:music@gayphx.com">music@gayphx.com</a>.</p>
                
                <p>Thank you for being part of the GayPHX community! 🌈</p>
                
                <hr style="margin: 30px 0; border: none; border-top: 1px solid #eee;">
                <p style="color: #666; font-size: 12px;">
                    This email was sent from the GayPHX Music Platform. If you didn't submit music to us, please ignore this email.
                </p>
            </div>
        </body>
        </html>
        """
        
        text_content = f"""
        Hello {artist_name}!
        
        Thank you for submitting your music to GayPHX! We've received your submission and our team will review it soon.
        
        Your Submission Details:
        - Tracking ID: {tracking_id}
        - Status: Pending Review
        
        Track your submission: {tracking_url}
        
        We'll notify you by email when your submission has been reviewed. If you have any questions, feel free to reach out to us at music@gayphx.com.
        
        Thank you for being part of the GayPHX community! 🌈
        """
        
        return self.send_email(to_email, subject, html_content, text_content)

    def send_isrc_assignment(self, to_email: str, artist_name: str, song_title: str, isrc_code: str, tracking_id: str) -> bool:
        """Send ISRC assignment notification"""
        tracking_url = f"{self.frontend_url}/track/{tracking_id}"
        
        subject = f"Your ISRC Code is Ready - {song_title}"
        
        html_content = f"""
        <html>
        <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
            <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; text-align: center;">
                <h1>🎵 GayPHX Music Platform</h1>
            </div>
            <div style="padding: 20px;">
                <h2>Congratulations {artist_name}!</h2>
                <p>Your song has been approved and your ISRC code has been assigned!</p>
                
                <div style="background: #d4edda; border: 1px solid #c3e6cb; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <h3>🎉 Your ISRC Code:</h3>
                    <p style="font-size: 24px; font-weight: bold; color: #155724; margin: 10px 0;">{isrc_code}</p>
                    <p><strong>Song:</strong> {song_title}</p>
                </div>
                
                <p>You can download your official ISRC certificate and view all your submissions in your artist dashboard:</p>
                <p><a href="{tracking_url}" style="background: #28a745; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block;">View Your Dashboard</a></p>
                
                <p>Your ISRC code is now registered and can be used for distribution, streaming, and royalty tracking.</p>
                
                <p>Thank you for being part of the GayPHX community! 🌈</p>
                
                <hr style="margin: 30px 0; border: none; border-top: 1px solid #eee;">
                <p style="color: #666; font-size: 12px;">
                    This email was sent from the GayPHX Music Platform.
                </p>
            </div>
        </body>
        </html>
        """
        
        text_content = f"""
        Congratulations {artist_name}!
        
        Your song has been approved and your ISRC code has been assigned!
        
        Your ISRC Code: {isrc_code}
        Song: {song_title}
        
        View your dashboard: {tracking_url}
        
        Your ISRC code is now registered and can be used for distribution, streaming, and royalty tracking.
        
        Thank you for being part of the GayPHX community! 🌈
        """
        
        return self.send_email(to_email, subject, html_content, text_content)

    def send_magic_link(self, to_email: str, token: str) -> bool:
        """Send magic link for artist login"""
        login_url = f"{self.frontend_url}/auth/verify?token={token}"
        
        subject = "Your GayPHX Music Dashboard Login Link"
        
        html_content = f"""
        <html>
        <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
            <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; text-align: center;">
                <h1>🎵 GayPHX Music Platform</h1>
            </div>
            <div style="padding: 20px;">
                <h2>Login to Your Dashboard</h2>
                <p>Click the link below to access your artist dashboard:</p>
                
                <p><a href="{login_url}" style="background: #667eea; color: white; padding: 15px 30px; text-decoration: none; border-radius: 5px; display: inline-block; font-size: 16px;">Access My Dashboard</a></p>
                
                <p style="color: #666; font-size: 14px;">This link will expire in 15 minutes for security reasons.</p>
                
                <p>If you didn't request this login link, please ignore this email.</p>
                
                <hr style="margin: 30px 0; border: none; border-top: 1px solid #eee;">
                <p style="color: #666; font-size: 12px;">
                    This email was sent from the GayPHX Music Platform.
                </p>
            </div>
        </body>
        </html>
        """
        
        text_content = f"""
        Login to Your Dashboard
        
        Click the link below to access your artist dashboard:
        {login_url}
        
        This link will expire in 15 minutes for security reasons.
        
        If you didn't request this login link, please ignore this email.
        """
        
        return self.send_email(to_email, subject, html_content, text_content)

    def send_welcome_email(self, to_email: str, artist_name: str, token: str) -> bool:
        """Send welcome email with magic link for new artist signup"""
        login_url = f"{self.frontend_url}/auth/verify?token={token}"
        
        subject = "Welcome to GayPHX Music! 🎵"
        
        html_content = f"""
        <html>
        <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
            <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; text-align: center;">
                <h1>🎵 Welcome to GayPHX Music!</h1>
            </div>
            <div style="padding: 20px;">
                <h2>Hello {artist_name}!</h2>
                <p>Welcome to the GayPHX Music community! We're excited to have you join us.</p>
                
                <div style="background: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <h3>What's Next?</h3>
                    <ul>
                        <li>Submit your music for review and ISRC assignment</li>
                        <li>Track your submissions in your personal dashboard</li>
                        <li>Get official ISRC codes for distribution</li>
                        <li>Connect with other artists in our community</li>
                    </ul>
                </div>
                
                <p>Click the link below to access your artist dashboard and start submitting music:</p>
                <p><a href="{login_url}" style="background: #667eea; color: white; padding: 15px 30px; text-decoration: none; border-radius: 5px; display: inline-block; font-size: 16px;">Access My Dashboard</a></p>
                
                <p style="color: #666; font-size: 14px;">This link will expire in 15 minutes for security reasons.</p>
                
                <p>If you have any questions, feel free to reach out to us at <a href="mailto:music@gayphx.com">music@gayphx.com</a>.</p>
                
                <p>Welcome to the GayPHX family! 🌈</p>
                
                <hr style="margin: 30px 0; border: none; border-top: 1px solid #eee;">
                <p style="color: #666; font-size: 12px;">
                    This email was sent from the GayPHX Music Platform.
                </p>
            </div>
        </body>
        </html>
        """
        
        text_content = f"""
        Welcome to GayPHX Music!
        
        Hello {artist_name}!
        
        Welcome to the GayPHX Music community! We're excited to have you join us.
        
        What's Next?
        - Submit your music for review and ISRC assignment
        - Track your submissions in your personal dashboard
        - Get official ISRC codes for distribution
        - Connect with other artists in our community
        
        Access your dashboard: {login_url}
        
        This link will expire in 15 minutes for security reasons.
        
        If you have any questions, feel free to reach out to us at music@gayphx.com.
        
        Welcome to the GayPHX family! 🌈
        """
        
        return self.send_email(to_email, subject, html_content, text_content)


# Global instance
email_service = EmailService()
