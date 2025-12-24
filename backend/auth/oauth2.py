"""
OAuth2 and JWT authentication implementation
"""

import os
from datetime import datetime, timedelta
from typing import Optional
from jose import JWTError, jwt
from passlib.context import CryptContext
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from backend.models.user import User

# Configuration
SECRET_KEY = os.getenv("JWT_SECRET_KEY")
if not SECRET_KEY or len(SECRET_KEY) < 32:
    raise ValueError(
        "JWT_SECRET_KEY environment variable must be set and at least 32 characters long. "
        "Generate a strong secret key for production use."
    )
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = int(os.getenv("ACCESS_TOKEN_EXPIRE_MINUTES", "480"))  # Default 8 hours (480 minutes) - can be overridden via environment variable

# Password hashing
# Use pbkdf2_sha256 to avoid bcrypt initialization bug detection issues
# pbkdf2_sha256 is still secure and doesn't have the 72-byte limit issue
pwd_context = CryptContext(schemes=["pbkdf2_sha256"], deprecated="auto")


def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verify a password against its hash"""
    # Truncate password to 72 bytes (bcrypt limit) if needed
    if len(plain_password.encode('utf-8')) > 72:
        plain_password = plain_password[:72]
    return pwd_context.verify(plain_password, hashed_password)


def get_password_hash(password: str) -> str:
    """Hash a password"""
    # Truncate password to 72 bytes (bcrypt limit) if needed
    if len(password.encode('utf-8')) > 72:
        password = password[:72]
    return pwd_context.hash(password)


def create_access_token(data: dict, expires_delta: Optional[timedelta] = None):
    """Create a JWT access token"""
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt


def verify_token(token: str, allow_expired: bool = False) -> Optional[dict]:
    """Verify and decode a JWT token
    
    Args:
        token: JWT token string
        allow_expired: If True, allow expired tokens (useful for refresh endpoint)
    """
    try:
        # Check if token is blacklisted (synchronous check for in-memory storage)
        from backend.services.token_blacklist_service import TokenBlacklistService, redis_client, _token_blacklist
        
        storage_key = TokenBlacklistService._get_storage_key(token)
        
        # Check blacklist (synchronous for in-memory, async for Redis handled in middleware)
        if redis_client:
            try:
                exists = redis_client.exists(storage_key)
                if exists:
                    return None  # Token is blacklisted
            except Exception:
                # Fall back to in-memory check on Redis error
                if storage_key in _token_blacklist:
                    expires_at = _token_blacklist[storage_key]
                    if datetime.utcnow() <= expires_at:
                        return None  # Token is blacklisted
        else:
            # In-memory storage check
            if storage_key in _token_blacklist:
                expires_at = _token_blacklist[storage_key]
                if datetime.utcnow() <= expires_at:
                    return None  # Token is blacklisted
                else:
                    # Remove expired token
                    del _token_blacklist[storage_key]
        
        # Decode token - if allow_expired is True, don't validate expiration
        if allow_expired:
            # Decode without expiration validation
            payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM], options={"verify_exp": False})
        else:
            # Normal decode with expiration validation
            payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        return payload
    except JWTError:
        return None


async def authenticate_user(db: AsyncSession, username: str, password: str) -> Optional[User]:
    """Authenticate a user with username and password"""
    result = await db.execute(select(User).where(User.username == username))
    user = result.scalar_one_or_none()
    
    if not user:
        return None
    
    if not verify_password(password, user.password_hash):
        return None
    
    return user


async def get_user_by_username(db: AsyncSession, username: str) -> Optional[User]:
    """Get a user by username"""
    result = await db.execute(select(User).where(User.username == username))
    return result.scalar_one_or_none()


async def get_user_by_id(db: AsyncSession, user_id: int) -> Optional[User]:
    """Get a user by ID"""
    result = await db.execute(select(User).where(User.id == user_id))
    return result.scalar_one_or_none()
