"""
Authentication router
"""

from fastapi import APIRouter, Depends, HTTPException, status, Request
from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm
from pydantic import BaseModel
from sqlalchemy.ext.asyncio import AsyncSession
from backend.database import get_db
from backend.auth.oauth2 import authenticate_user, create_access_token, verify_token, get_user_by_username
from backend.models.user import User
from backend.logging.audit import audit_logger

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


@router.post("/login")
async def login(
    request: Request,
    login_data: LoginRequest,
    db: AsyncSession = Depends(get_db)
):
    """Login endpoint - accepts JSON or form data"""
    user = await authenticate_user(db, login_data.username, login_data.password)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    access_token = create_access_token(data={"sub": user.username})
    
    # Log login
    client_ip = request.client.host if request.client else None
    user_agent = request.headers.get("user-agent")
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
    # Log logout
    client_ip = request.client.host if request.client else None
    audit_logger.log_logout(current_user.id, client_ip)
    
    return {"message": "Successfully logged out"}
