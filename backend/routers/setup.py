"""
Setup and configuration endpoints
"""

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from backend.database import get_db
from backend.models.user import User
from backend.auth.oauth2 import get_password_hash
from backend.auth.password_validator import validate_password, PasswordValidationError
from backend.routers.auth import get_current_user
from backend.scripts.setup import create_initial_admin, check_api_keys
from pydantic import BaseModel
import os

router = APIRouter()


class SetupStatus(BaseModel):
    """Setup status response"""
    admin_user_exists: bool
    api_keys_configured: bool
    libretime_configured: bool
    azuracast_configured: bool


class CreateUserRequest(BaseModel):
    """Create user request"""
    username: str
    password: str
    role: str = "admin"


@router.get("/status")
async def get_setup_status(db: AsyncSession = Depends(get_db)):
    """Get current setup status"""
    from sqlalchemy import select, func
    from backend.models.user import User
    
    # Check if admin users exist
    result = await db.execute(
        select(func.count(User.id)).where(User.role == "admin")
    )
    admin_count = result.scalar() or 0
    admin_user_exists = admin_count > 0
    
    # Check API keys
    libretime_key = os.getenv("LIBRETIME_API_KEY")
    azuracast_key = os.getenv("AZURACAST_API_KEY")
    
    libretime_configured = libretime_key and libretime_key != "changeme"
    azuracast_configured = azuracast_key and azuracast_key != "changeme"
    api_keys_configured = libretime_configured and azuracast_configured
    
    return SetupStatus(
        admin_user_exists=admin_user_exists,
        api_keys_configured=api_keys_configured,
        libretime_configured=libretime_configured,
        azuracast_configured=azuracast_configured
    )


@router.post("/create-admin")
async def create_admin_user(
    request: CreateUserRequest,
    db: AsyncSession = Depends(get_db)
):
    """Create initial admin user (only if no admin exists)"""
    from sqlalchemy import select, func
    from backend.models.user import User
    
    # Check if admin users already exist
    result = await db.execute(
        select(func.count(User.id)).where(User.role == "admin")
    )
    admin_count = result.scalar() or 0
    
    if admin_count > 0:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Admin users already exist"
        )
    
    # Validate password complexity
    try:
        validate_password(request.password)
    except PasswordValidationError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e)
        )
    
    # Create admin user
    admin_user = User(
        username=request.username,
        password_hash=get_password_hash(request.password),
        role=request.role
    )
    
    db.add(admin_user)
    await db.commit()
    await db.refresh(admin_user)
    
    return {"message": "Admin user created successfully"}


@router.get("/api-keys")
async def get_api_key_status():
    """Get API key configuration status"""
    libretime_key = os.getenv("LIBRETIME_API_KEY")
    azuracast_key = os.getenv("AZURACAST_API_KEY")
    
    return {
        "libretime_configured": bool(libretime_key and libretime_key != "changeme"),
        "azuracast_configured": bool(azuracast_key and azuracast_key != "changeme"),
        "libretime_url": os.getenv("LIBRETIME_URL", ""),
        "azuracast_url": os.getenv("AZURACAST_URL", "")
    }


@router.post("/run-initial-setup")
async def run_initial_setup():
    """Run initial setup (create admin user)"""
    await create_initial_admin()
    api_keys_ok = await check_api_keys()
    
    return {
        "message": "Initial setup completed",
        "api_keys_configured": api_keys_ok
    }
