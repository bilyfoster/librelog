"""
Authentication router
"""

from fastapi import APIRouter, Depends, HTTPException, status, Request
from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm
from pydantic import BaseModel
from typing import Optional
from datetime import datetime
from sqlalchemy.ext.asyncio import AsyncSession
from backend.database import get_db
from backend.auth.oauth2 import authenticate_user, create_access_token, verify_token, get_user_by_username
from backend.auth.password_validator import validate_password, PasswordValidationError
from backend.models.user import User
from backend.logging.audit import audit_logger
from backend.services.auth_security_service import AuthSecurityService, MAX_FAILED_ATTEMPTS

router = APIRouter()
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="token")


class LoginRequest(BaseModel):
    """Login request model"""
    username: str
    password: str


async def get_current_user(
    token: str = Depends(oauth2_scheme),
    db: AsyncSession = Depends(get_db)
) -> User:
    """Get current authenticated user"""
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    
    payload = verify_token(token)
    if payload is None:
        raise credentials_exception
    
    username: str = payload.get("sub")
    if username is None:
        raise credentials_exception
    
    user = await get_user_by_username(db, username)
    if user is None:
        raise credentials_exception
    
    return user


class UserProfileUpdate(BaseModel):
    """User profile update model"""
    username: Optional[str] = None
    password: Optional[str] = None
    current_password: Optional[str] = None  # Required when changing password


@router.get("/profile", response_model=dict)
async def get_user_profile(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """Get current user's profile"""
    return {
        "id": current_user.id,
        "username": current_user.username,
        "role": current_user.role,
        "created_at": current_user.created_at.isoformat() if current_user.created_at else None,
        "last_login": current_user.last_login.isoformat() if current_user.last_login else None,
        "last_activity": current_user.last_activity.isoformat() if current_user.last_activity else None,
    }


@router.put("/profile", response_model=dict)
async def update_user_profile(
    profile_data: UserProfileUpdate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """Update current user's profile"""
    import logging
    logger = logging.getLogger(__name__)
    
    try:
        from backend.auth.oauth2 import verify_password, get_password_hash
        
        # Check username uniqueness if changing username
        if profile_data.username is not None and profile_data.username != current_user.username:
            from sqlalchemy import select
            result = await db.execute(select(User).where(User.username == profile_data.username))
            existing = result.scalar_one_or_none()
            if existing:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Username already exists"
                )
            current_user.username = profile_data.username
        
        # Handle password change
        if profile_data.password is not None:
            if not profile_data.current_password:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Current password is required to change password"
                )
            
            # Verify current password
            if not current_user.password_hash:
                logger.error(f"User {current_user.id} has no password hash")
                raise HTTPException(
                    status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                    detail="User password hash is missing"
                )
            
            if not verify_password(profile_data.current_password, current_user.password_hash):
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Current password is incorrect"
                )
            
            # Validate new password complexity
            try:
                validate_password(profile_data.password)
            except PasswordValidationError as e:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail=str(e)
                )
            
            # Set new password
            current_user.password_hash = get_password_hash(profile_data.password)
        
        # Only commit if there were changes
        if profile_data.username is not None or profile_data.password is not None:
            await db.commit()
            await db.refresh(current_user)
        
        # Log audit action (non-blocking)
        try:
            audit_logger.info(
                "User profile updated",
                user_id=current_user.id,
                username=current_user.username
            )
        except Exception as e:
            logger.warning(f"Failed to log audit action: {e}")
        
        return {
            "id": current_user.id,
            "username": current_user.username,
            "role": current_user.role,
            "created_at": current_user.created_at.isoformat() if current_user.created_at else None,
            "last_login": current_user.last_login.isoformat() if current_user.last_login else None,
            "last_activity": current_user.last_activity.isoformat() if current_user.last_activity else None,
        }
    except HTTPException:
        # Re-raise HTTP exceptions as-is
        raise
    except Exception as e:
        logger.error(f"Error updating user profile: {e}", exc_info=True)
        await db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to update profile: {str(e)}"
        )


@router.post("/login")
async def login(
    request: Request,
    login_data: LoginRequest,
    db: AsyncSession = Depends(get_db)
):
    """Login endpoint - accepts JSON or form data"""
    client_ip = request.client.host if request.client else None
    user_agent = request.headers.get("user-agent")
    
    # Check if account is locked
    auth_security = AuthSecurityService(db)
    is_locked, lockout_until = await auth_security.is_account_locked(login_data.username)
    
    if is_locked:
        remaining_minutes = int((lockout_until - datetime.utcnow()).total_seconds() / 60) + 1
        raise HTTPException(
            status_code=status.HTTP_423_LOCKED,
            detail=f"Account is locked due to too many failed login attempts. "
                   f"Please try again in {remaining_minutes} minute(s).",
            headers={"WWW-Authenticate": "Bearer", "Retry-After": str(remaining_minutes * 60)},
        )
    
    # Check if CAPTCHA should be shown (informational, actual CAPTCHA implementation is optional)
    should_show_captcha = await auth_security.should_show_captcha(login_data.username)
    
    # Attempt authentication
    user = await authenticate_user(db, login_data.username, login_data.password)
    
    if not user:
        # Record failed login attempt
        await auth_security.record_failed_attempt(
            username=login_data.username,
            ip_address=client_ip,
            user_agent=user_agent
        )
        
        # Log failed login attempt
        audit_logger.warning(
            "Failed login attempt",
            username=login_data.username,
            ip_address=client_ip,
            user_agent=user_agent
        )
        
        # Check if account is now locked after this failed attempt
        is_locked, lockout_until = await auth_security.is_account_locked(login_data.username)
        if is_locked:
            remaining_minutes = int((lockout_until - datetime.utcnow()).total_seconds() / 60) + 1
            
            # Send notification to user about account lockout (if user exists)
            try:
                locked_user = await get_user_by_username(db, login_data.username)
                if locked_user:
                    from backend.services.notification_service import NotificationService
                    from backend.models.notification import NotificationType
                    await NotificationService.create_notification(
                        db=db,
                        user_id=locked_user.id,
                        message=f"Your account has been locked due to {MAX_FAILED_ATTEMPTS} failed login attempts. "
                               f"Please try again in {remaining_minutes} minute(s).",
                        notification_type=NotificationType.IN_APP,
                        subject="Account Locked"
                    )
            except Exception as e:
                logger.warning("Failed to send lockout notification", error=str(e), exc_info=True)
            
            raise HTTPException(
                status_code=status.HTTP_423_LOCKED,
                detail=f"Account has been locked due to too many failed login attempts. "
                       f"Please try again in {remaining_minutes} minute(s).",
                headers={"WWW-Authenticate": "Bearer", "Retry-After": str(remaining_minutes * 60)},
            )
        
        error_detail = "Incorrect username or password"
        if should_show_captcha:
            error_detail += ". CAPTCHA verification may be required."
        
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=error_detail,
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    # Successful login - clear failed attempts
    await auth_security.clear_failed_attempts(login_data.username)
    
    access_token = create_access_token(data={"sub": user.username})
    
    # Log successful login
    audit_logger.log_login(user.id, client_ip, user_agent)
    
    return {"access_token": access_token, "token_type": "bearer"}


@router.get("/me")
async def read_users_me(current_user: User = Depends(get_current_user)):
    """Get current user info"""
    return {
        "id": current_user.id,
        "username": current_user.username,
        "role": current_user.role,
        "created_at": current_user.created_at,
        "last_login": current_user.last_login
    }


@router.post("/refresh")
async def refresh_token(
    request: Request,
    current_user: User = Depends(get_current_user)
):
    """Refresh JWT token - returns new access token"""
    # Create new token with same user
    new_access_token = create_access_token(data={"sub": current_user.username})
    
    return {"access_token": new_access_token, "token_type": "bearer"}


@router.post("/logout")
async def logout(
    request: Request,
    current_user: User = Depends(get_current_user)
):
    """Logout endpoint"""
    # Get token from Authorization header
    authorization = request.headers.get("Authorization")
    if authorization:
        try:
            scheme, token = authorization.split()
            if scheme.lower() == "bearer":
                # Add token to blacklist
                from backend.services.token_blacklist_service import TokenBlacklistService
                from backend.auth.oauth2 import verify_token
                
                # Get token expiration from payload
                payload = verify_token(token)
                if payload and "exp" in payload:
                    from datetime import datetime
                    expires_at = datetime.utcfromtimestamp(payload["exp"])
                    await TokenBlacklistService.add_to_blacklist(token, expires_at)
        except (ValueError, AttributeError):
            pass  # Ignore errors in token extraction
    
    # Log logout
    client_ip = request.client.host if request.client else None
    audit_logger.log_logout(current_user.id, client_ip)
    
    return {"message": "Successfully logged out"}
