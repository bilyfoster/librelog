"""
Authentication security service for account lockout and failed login tracking
"""

import os
from datetime import datetime, timedelta, timezone
from typing import Optional, Tuple
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func, and_
from backend.models.failed_login_attempt import FailedLoginAttempt
import structlog

logger = structlog.get_logger()

# Configuration from environment variables
MAX_FAILED_ATTEMPTS = int(os.getenv("ACCOUNT_LOCKOUT_ATTEMPTS", "5"))
LOCKOUT_DURATION_MINUTES = int(os.getenv("ACCOUNT_LOCKOUT_DURATION_MINUTES", "15"))
CAPTCHA_THRESHOLD = 3  # Show CAPTCHA after 3 failed attempts


class AuthSecurityService:
    """Service for managing authentication security features"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def record_failed_attempt(
        self,
        username: str,
        ip_address: Optional[str] = None,
        user_agent: Optional[str] = None
    ) -> None:
        """
        Record a failed login attempt.
        
        Args:
            username: Username that failed to authenticate
            ip_address: IP address of the login attempt
            user_agent: User agent string of the login attempt
        """
        failed_attempt = FailedLoginAttempt(
            username=username,
            ip_address=ip_address,
            user_agent=user_agent,
            attempted_at=datetime.now(timezone.utc)
        )
        self.db.add(failed_attempt)
        await self.db.commit()
        
        logger.warning(
            "Failed login attempt recorded",
            username=username,
            ip_address=ip_address
        )
    
    async def clear_failed_attempts(self, username: str) -> None:
        """
        Clear all failed login attempts for a user (called on successful login).
        
        Args:
            username: Username to clear attempts for
        """
        await self.db.execute(
            FailedLoginAttempt.__table__.delete().where(
                FailedLoginAttempt.username == username
            )
        )
        await self.db.commit()
        
        logger.info("Failed login attempts cleared", username=username)
    
    async def is_account_locked(self, username: str) -> Tuple[bool, Optional[datetime]]:
        """
        Check if an account is locked due to too many failed login attempts.
        
        Args:
            username: Username to check
            
        Returns:
            Tuple of (is_locked, lockout_until)
            - is_locked: True if account is currently locked
            - lockout_until: Datetime when lockout expires, None if not locked
        """
        # Calculate the time window for failed attempts
        lockout_window_start = datetime.now(timezone.utc) - timedelta(minutes=LOCKOUT_DURATION_MINUTES)
        
        # Count failed attempts within the lockout window
        result = await self.db.execute(
            select(func.count(FailedLoginAttempt.id)).where(
                and_(
                    FailedLoginAttempt.username == username,
                    FailedLoginAttempt.attempted_at >= lockout_window_start
                )
            )
        )
        failed_count = result.scalar() or 0
        
        if failed_count >= MAX_FAILED_ATTEMPTS:
            # Find the most recent failed attempt to calculate lockout expiration
            result = await self.db.execute(
                select(FailedLoginAttempt.attempted_at)
                .where(
                    and_(
                        FailedLoginAttempt.username == username,
                        FailedLoginAttempt.attempted_at >= lockout_window_start
                    )
                )
                .order_by(FailedLoginAttempt.attempted_at.desc())
                .limit(1)
            )
            most_recent_attempt = result.scalar_one_or_none()
            
            if most_recent_attempt:
                lockout_until = most_recent_attempt + timedelta(minutes=LOCKOUT_DURATION_MINUTES)
                # Check if lockout period has expired
                if datetime.now(timezone.utc) < lockout_until:
                    return True, lockout_until
                else:
                    # Lockout expired, clear old attempts
                    await self.clear_failed_attempts(username)
                    return False, None
        
        return False, None
    
    async def should_show_captcha(self, username: str) -> bool:
        """
        Check if CAPTCHA should be shown for login attempts.
        
        Args:
            username: Username to check
            
        Returns:
            True if CAPTCHA should be shown (3+ failed attempts in lockout window)
        """
        lockout_window_start = datetime.now(timezone.utc) - timedelta(minutes=LOCKOUT_DURATION_MINUTES)
        
        result = await self.db.execute(
            select(func.count(FailedLoginAttempt.id)).where(
                and_(
                    FailedLoginAttempt.username == username,
                    FailedLoginAttempt.attempted_at >= lockout_window_start
                )
            )
        )
        failed_count = result.scalar() or 0
        
        return failed_count >= CAPTCHA_THRESHOLD
    
    async def get_failed_attempt_count(self, username: str) -> int:
        """
        Get the number of failed login attempts for a user within the lockout window.
        
        Args:
            username: Username to check
            
        Returns:
            Number of failed attempts
        """
        lockout_window_start = datetime.now(timezone.utc) - timedelta(minutes=LOCKOUT_DURATION_MINUTES)
        
        result = await self.db.execute(
            select(func.count(FailedLoginAttempt.id)).where(
                and_(
                    FailedLoginAttempt.username == username,
                    FailedLoginAttempt.attempted_at >= lockout_window_start
                )
            )
        )
        return result.scalar() or 0


