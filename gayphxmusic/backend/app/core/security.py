from jose import JWTError, jwt
from datetime import datetime, timedelta
from typing import Optional
from fastapi import HTTPException, status, Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.models.artist import Artist
from app.models.admin_user import AdminUser
from app.core.config import settings
import uuid

security = HTTPBearer()

def create_access_token(data: dict, expires_delta: Optional[timedelta] = None):
    """Create JWT access token"""
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(minutes=settings.jwt_expire_minutes)
    
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, settings.jwt_secret, algorithm=settings.jwt_algorithm)
    return encoded_jwt

def verify_token(token: str) -> dict:
    """Verify JWT token and return payload"""
    try:
        payload = jwt.decode(token, settings.jwt_secret, algorithms=[settings.jwt_algorithm])
        return payload
    except JWTError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Could not validate credentials",
            headers={"WWW-Authenticate": "Bearer"},
        )

def get_current_artist(credentials: HTTPAuthorizationCredentials = Depends(security), db: Session = Depends(get_db)) -> Artist:
    """Get current artist from JWT token"""
    token = credentials.credentials
    
    # Add debug logging
    print(f"DEBUG: Received token: {token[:20]}...")
    
    # Try to decode as proper JWT first
    try:
        payload = verify_token(token)
        artist_id = payload.get("sub")
        is_artist = payload.get("is_artist", False)
        
        print(f"DEBUG: JWT payload - artist_id: {artist_id}, is_artist: {is_artist}")
        
        if not artist_id or not is_artist:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid token payload"
            )
    except HTTPException:
        raise
    except Exception as e:
        print(f"DEBUG: JWT decode failed: {e}")
        # Handle legacy simple tokens for backward compatibility
        if token.startswith("artist_token_"):
            # For legacy tokens, try to find any active artist (this is a fallback for testing)
            artist = db.query(Artist).filter(Artist.is_active == True).first()
            if not artist:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="No active artist found"
                )
            return artist
        else:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid token"
            )
    
    artist = db.query(Artist).filter(Artist.id == artist_id).first()
    if not artist:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Artist not found"
        )
    
    return artist

def get_current_admin(credentials: HTTPAuthorizationCredentials = Depends(security), db: Session = Depends(get_db)) -> AdminUser:
    """Get current admin from JWT token"""
    token = credentials.credentials
    
    try:
        payload = verify_token(token)
        admin_id = payload.get("sub")
        is_admin = payload.get("is_admin", False)
        
        if not is_admin:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Admin access required"
            )
            
    except HTTPException:
        raise
    except Exception:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token"
        )
    
    admin = db.query(AdminUser).filter(AdminUser.id == admin_id).first()
    if not admin:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Admin not found"
        )
    
    if not admin.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Admin account is disabled"
        )
    
    return admin

def get_password_hash(password: str) -> str:
    """Hash password using bcrypt"""
    from passlib.context import CryptContext
    pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
    return pwd_context.hash(password)

def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verify password against hash"""
    from passlib.context import CryptContext
    pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
    return pwd_context.verify(plain_password, hashed_password)