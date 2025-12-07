"""
Backups router for backup and restore operations
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query, BackgroundTasks
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, desc
from typing import List, Optional
from pydantic import BaseModel

from backend.database import get_db
from backend.models.backup import Backup, BackupType, BackupStatus, StorageProvider
from backend.models.user import User
from backend.routers.auth import get_current_user
from backend.models.user_roles import has_permission, Permission
from backend.services.backup_service import BackupService

router = APIRouter(prefix="/backups", tags=["backups"])


class BackupCreate(BaseModel):
    backup_type: BackupType
    storage_provider: StorageProvider
    description: Optional[str] = None


class BackupResponse(BaseModel):
    id: int
    backup_type: BackupType
    status: BackupStatus
    storage_provider: StorageProvider
    filename: str
    file_path: Optional[str] = None
    remote_path: Optional[str] = None
    file_size: Optional[float] = None
    database_dump: bool
    files_included: bool
    description: Optional[str] = None
    started_at: Optional[str] = None
    completed_at: Optional[str] = None
    error_message: Optional[str] = None
    created_by: int
    created_at: str

    class Config:
        from_attributes = True


class RestoreRequest(BaseModel):
    restore_database: bool = True
    restore_files: bool = True


@router.get("/", response_model=List[BackupResponse])
async def list_backups(
    skip: int = Query(0, ge=0),
    limit: int = Query(50, ge=1, le=100),
    status_filter: Optional[BackupStatus] = None,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all backups"""
    query = select(Backup)
    
    if status_filter:
        query = query.where(Backup.status == status_filter)
    
    query = query.order_by(desc(Backup.created_at)).offset(skip).limit(limit)
    
    result = await db.execute(query)
    backups = result.scalars().all()
    return backups


@router.get("/{backup_id}", response_model=BackupResponse)
async def get_backup(
    backup_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific backup"""
    result = await db.execute(select(Backup).where(Backup.id == backup_id))
    backup = result.scalar_one_or_none()
    
    if not backup:
        raise HTTPException(status_code=404, detail="Backup not found")
    
    return backup


@router.post("/", response_model=BackupResponse, status_code=status.HTTP_201_CREATED)
@router.post("/create", response_model=BackupResponse, status_code=status.HTTP_201_CREATED)
async def create_backup(
    backup: BackupCreate,
    background_tasks: BackgroundTasks,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new backup"""
    new_backup = await BackupService.create_backup(
        db,
        backup.backup_type,
        backup.storage_provider,
        backup.description,
        current_user.id
    )
    
    return new_backup


@router.post("/{backup_id}/restore", status_code=status.HTTP_200_OK)
async def restore_backup(
    backup_id: int,
    restore_request: RestoreRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Restore from a backup (admin only)"""
    # Only admins can restore backups
    if not has_permission(current_user, Permission.DELETE_USERS):  # Using DELETE_USERS as admin-only permission
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only admins can restore backups"
        )
    
    try:
        result = await BackupService.restore_backup(
            db,
            backup_id,
            restore_request.restore_database,
            restore_request.restore_files
        )
        return result
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Restore failed: {str(e)}")


@router.delete("/{backup_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_backup(
    backup_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete a backup"""
    result = await db.execute(select(Backup).where(Backup.id == backup_id))
    backup = result.scalar_one_or_none()
    
    if not backup:
        raise HTTPException(status_code=404, detail="Backup not found")
    
    # Delete file if local
    if backup.storage_provider == StorageProvider.LOCAL and backup.file_path:
        import os
        if os.path.exists(backup.file_path):
            os.remove(backup.file_path)
    
    # TODO: Delete from S3/B2 if remote
    
    await db.delete(backup)
    await db.commit()

