"""
User management router
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, update
from sqlalchemy.orm import selectinload
from typing import List, Optional
from pydantic import BaseModel, EmailStr
import structlog

from backend.database import get_db
from backend.models.user import User
from backend.routers.auth import get_current_user
from backend.auth.oauth2 import get_password_hash
from backend.models.user_roles import Role, Permission, has_permission, Permission as Perm

logger = structlog.get_logger()

router = APIRouter(prefix="/users", tags=["users"])


class UserCreate(BaseModel):
    username: str
    password: str
    role: str


class UserUpdate(BaseModel):
    username: Optional[str] = None
    password: Optional[str] = None
    role: Optional[str] = None


class UserResponse(BaseModel):
    id: int
    username: str
    role: str
    created_at: str
    last_login: Optional[str] = None
    last_activity: Optional[str] = None

    class Config:
        from_attributes = True


def require_admin(current_user: User = Depends(get_current_user)) -> User:
    """Require admin role"""
    if current_user.role != "admin":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Admin access required"
        )
    return current_user


@router.get("/", response_model=List[UserResponse])
async def list_users(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    role_filter: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(require_admin)
):
    """List all users (admin only)"""
    try:
        query = select(User)
        
        if role_filter:
            query = query.where(User.role == role_filter)
        
        query = query.offset(skip).limit(limit).order_by(User.created_at.desc())
        
        result = await db.execute(query)
        users = result.scalars().all()
        
        # Convert datetime fields to strings for Pydantic validation
        users_data = []
        for user in users:
            user_dict = {
                "id": user.id,
                "username": user.username,
                "role": user.role,
                "created_at": user.created_at.isoformat() if user.created_at else None,
                "last_login": user.last_login.isoformat() if user.last_login else None,
                "last_activity": user.last_activity.isoformat() if user.last_activity else None,
            }
            users_data.append(UserResponse(**user_dict))
        
        return users_data
    except Exception as e:
        logger.error("Failed to list users", error=str(e), exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to list users: {str(e)}"
        )


@router.get("/{user_id}", response_model=UserResponse)
async def get_user(
    user_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(require_admin)
):
    """Get a specific user (admin only)"""
    try:
        result = await db.execute(select(User).where(User.id == user_id))
        user = result.scalar_one_or_none()
        
        if not user:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User not found"
            )
        
        # Convert datetime fields to strings for Pydantic validation
        user_dict = {
            "id": user.id,
            "username": user.username,
            "role": user.role,
            "created_at": user.created_at.isoformat() if user.created_at else None,
            "last_login": user.last_login.isoformat() if user.last_login else None,
            "last_activity": user.last_activity.isoformat() if user.last_activity else None,
        }
        return UserResponse(**user_dict)
    except HTTPException:
        raise
    except Exception as e:
        logger.error("Failed to get user", user_id=user_id, error=str(e), exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to get user: {str(e)}"
        )


@router.post("/", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
async def create_user(
    user_data: UserCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(require_admin)
):
    """Create a new user (admin only)"""
    try:
        # Validate role
        valid_roles = ["admin", "producer", "dj", "sales"]
        if user_data.role not in valid_roles:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Invalid role. Must be one of: {', '.join(valid_roles)}"
            )
        
        # Check if username already exists
        result = await db.execute(select(User).where(User.username == user_data.username))
        existing = result.scalar_one_or_none()
        if existing:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Username already exists"
            )
        
        # Create user
        password_hash = get_password_hash(user_data.password)
        new_user = User(
            username=user_data.username,
            password_hash=password_hash,
            role=user_data.role
        )
        db.add(new_user)
        await db.commit()
        await db.refresh(new_user)
        
        logger.info("User created", user_id=new_user.id, username=new_user.username, role=new_user.role, created_by=current_user.id)
        
        # Convert datetime fields to strings for Pydantic validation
        user_dict = {
            "id": new_user.id,
            "username": new_user.username,
            "role": new_user.role,
            "created_at": new_user.created_at.isoformat() if new_user.created_at else None,
            "last_login": new_user.last_login.isoformat() if new_user.last_login else None,
            "last_activity": new_user.last_activity.isoformat() if new_user.last_activity else None,
        }
        return UserResponse(**user_dict)
    except HTTPException:
        raise
    except Exception as e:
        await db.rollback()
        logger.error("Failed to create user", error=str(e), exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to create user: {str(e)}"
        )


@router.put("/{user_id}", response_model=UserResponse)
async def update_user(
    user_id: int,
    user_data: UserUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(require_admin)
):
    """Update a user (admin only)"""
    try:
        result = await db.execute(select(User).where(User.id == user_id))
        user = result.scalar_one_or_none()
        
        if not user:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User not found"
            )
        
        # Validate role if provided
        if user_data.role is not None:
            valid_roles = ["admin", "producer", "dj", "sales"]
            if user_data.role not in valid_roles:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail=f"Invalid role. Must be one of: {', '.join(valid_roles)}"
                )
        
        # Check username uniqueness if changing username
        if user_data.username is not None and user_data.username != user.username:
            result = await db.execute(select(User).where(User.username == user_data.username))
            existing = result.scalar_one_or_none()
            if existing:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Username already exists"
                )
        
        # Update fields
        update_data = user_data.model_dump(exclude_unset=True)
        if "password" in update_data:
            update_data["password_hash"] = get_password_hash(update_data.pop("password"))
        
        for field, value in update_data.items():
            setattr(user, field, value)
        
        await db.commit()
        await db.refresh(user)
        
        logger.info("User updated", user_id=user_id, updated_by=current_user.id)
        
        # Convert datetime fields to strings for Pydantic validation
        user_dict = {
            "id": user.id,
            "username": user.username,
            "role": user.role,
            "created_at": user.created_at.isoformat() if user.created_at else None,
            "last_login": user.last_login.isoformat() if user.last_login else None,
            "last_activity": user.last_activity.isoformat() if user.last_activity else None,
        }
        return UserResponse(**user_dict)
    except HTTPException:
        raise
    except Exception as e:
        await db.rollback()
        logger.error("Failed to update user", user_id=user_id, error=str(e), exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to update user: {str(e)}"
        )


@router.delete("/{user_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_user(
    user_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(require_admin)
):
    """Delete a user (admin only)"""
    try:
        # Prevent deleting yourself
        if user_id == current_user.id:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Cannot delete your own account"
            )
        
        result = await db.execute(select(User).where(User.id == user_id))
        user = result.scalar_one_or_none()
        
        if not user:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User not found"
            )
        
        await db.delete(user)
        await db.commit()
        
        logger.info("User deleted", user_id=user_id, deleted_by=current_user.id)
        
        return None
    except HTTPException:
        raise
    except Exception as e:
        await db.rollback()
        logger.error("Failed to delete user", user_id=user_id, error=str(e), exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to delete user: {str(e)}"
        )

