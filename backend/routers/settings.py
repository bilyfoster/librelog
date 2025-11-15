"""
Settings router for application configuration management
"""

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from typing import Dict, Any, Optional
from pydantic import BaseModel, EmailStr

from backend.database import get_db
from backend.models.user import User
from backend.routers.auth import get_current_user
from backend.services.settings_service import SettingsService

router = APIRouter(prefix="/settings", tags=["settings"])


class SettingValue(BaseModel):
    value: str
    encrypted: bool = False
    description: Optional[str] = None


class CategorySettings(BaseModel):
    settings: Dict[str, SettingValue]


class SMTPTestRequest(BaseModel):
    host: str
    port: int
    username: str
    password: str
    use_tls: bool = True


class S3TestRequest(BaseModel):
    access_key_id: str
    secret_access_key: str
    bucket_name: str
    region: str = "us-east-1"


class BackblazeTestRequest(BaseModel):
    application_key_id: str
    application_key: str
    bucket_name: str


@router.get("/")
async def get_all_settings(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get all settings grouped by category"""
    import os
    categories = ["general", "smtp", "storage", "backup", "integrations"]
    result = {}
    
    for category in categories:
        result[category] = await SettingsService.get_category_settings(db, category)
    
    # Override integrations settings with environment variables if they exist
    # This ensures the UI shows the actual values being used
    libretime_url = os.getenv("LIBRETIME_URL", "").rstrip("/api/v2").rstrip("/")
    libretime_api_key = os.getenv("LIBRETIME_API_KEY", "")
    
    if libretime_url:
        if "integrations" not in result:
            result["integrations"] = {}
        result["integrations"]["libretime_url"] = {
            "value": libretime_url,
            "encrypted": False,
            "description": "LibreTime API URL (from environment variable)"
        }
    
    if libretime_api_key:
        if "integrations" not in result:
            result["integrations"] = {}
        result["integrations"]["libretime_api_key"] = {
            "value": libretime_api_key[:10] + "..." if len(libretime_api_key) > 10 else libretime_api_key,
            "encrypted": True,
            "description": "LibreTime API Key (from environment variable, masked)"
        }
    
    return result


@router.get("/{category}")
async def get_category_settings(
    category: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get settings for a specific category"""
    settings = await SettingsService.get_category_settings(db, category)
    return settings


@router.put("/{category}")
async def update_category_settings(
    category: str,
    settings_data: CategorySettings,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update settings for a specific category"""
    settings_dict = {
        key: {
            "value": value.value,
            "encrypted": value.encrypted,
            "description": value.description,
        }
        for key, value in settings_data.settings.items()
    }
    
    updated = await SettingsService.update_category_settings(db, category, settings_dict)
    return {"status": "success", "updated": len(updated)}


@router.post("/test-smtp")
async def test_smtp_connection(
    test_data: SMTPTestRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Test SMTP connection"""
    result = await SettingsService.test_smtp_connection(
        test_data.host,
        test_data.port,
        test_data.username,
        test_data.password,
        test_data.use_tls
    )
    return result


@router.post("/test-s3")
async def test_s3_connection(
    test_data: S3TestRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Test S3 connection"""
    result = await SettingsService.test_s3_connection(
        test_data.access_key_id,
        test_data.secret_access_key,
        test_data.bucket_name,
        test_data.region
    )
    return result


@router.post("/test-backblaze")
async def test_backblaze_connection(
    test_data: BackblazeTestRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Test Backblaze B2 connection"""
    result = await SettingsService.test_backblaze_connection(
        test_data.application_key_id,
        test_data.application_key,
        test_data.bucket_name
    )
    return result

