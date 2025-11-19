"""
Settings router for application configuration management
"""

from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File
from fastapi.responses import FileResponse
from sqlalchemy.ext.asyncio import AsyncSession
from typing import Dict, Any, Optional
from pydantic import BaseModel, EmailStr
from pathlib import Path
import os
import aiofiles
from datetime import datetime

from backend.database import get_db
from backend.models.user import User
from backend.routers.auth import get_current_user
from backend.services.settings_service import SettingsService

router = APIRouter(prefix="/settings", tags=["settings"])

# Logo storage directory - create lazily with fallback
def _get_logo_dir() -> Path:
    """Get logo directory, creating it if needed with fallback"""
    logo_dir = Path(os.getenv("LOGO_DIR", "/var/lib/librelog/logos"))
    try:
        logo_dir.mkdir(parents=True, exist_ok=True)
        return logo_dir
    except (PermissionError, FileNotFoundError, OSError):
        # Fallback to /tmp if /var/lib is not writable
        fallback_dir = Path("/tmp/librelog/logos")
        try:
            fallback_dir.mkdir(parents=True, exist_ok=True)
            return fallback_dir
        except Exception:
            # Last resort: use /tmp directly
            Path("/tmp").mkdir(parents=True, exist_ok=True)
            return Path("/tmp")


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
    categories = ["general", "branding", "smtp", "storage", "backup", "integrations"]
    result = {}
    
    for category in categories:
        result[category] = await SettingsService.get_category_settings(db, category)
    
    # Set default branding values only if not set in database
    branding_settings = result.get("branding", {})
    if not branding_settings.get("system_name") or not branding_settings["system_name"].get("value"):
        branding_settings["system_name"] = {
            "value": "GayPHX Radio Traffic System",
            "encrypted": False,
            "description": "Name of the traffic system displayed in the header"
        }
    if not branding_settings.get("header_color") or not branding_settings["header_color"].get("value"):
        branding_settings["header_color"] = {
            "value": "#424242",
            "encrypted": False,
            "description": "Header background color (hex code). Ensure API status indicators (green/red) remain visible."
        }
    if "logo_url" not in branding_settings:
        branding_settings["logo_url"] = {
            "value": "",
            "encrypted": False,
            "description": "URL to the logo image file (uploaded via logo upload endpoint)"
        }
    result["branding"] = branding_settings
    
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
    
    # For branding category, apply defaults if not set
    if category == "branding":
        if not settings.get("system_name") or not settings["system_name"].get("value"):
            settings["system_name"] = {
                "value": "GayPHX Radio Traffic System",
                "encrypted": False,
                "description": "Name of the traffic system displayed in the header"
            }
        if not settings.get("header_color") or not settings["header_color"].get("value"):
            settings["header_color"] = {
                "value": "#424242",
                "encrypted": False,
                "description": "Header background color (hex code). Ensure API status indicators (green/red) remain visible."
            }
        if "logo_url" not in settings:
            settings["logo_url"] = {
                "value": "",
                "encrypted": False,
                "description": "URL to the logo image file (uploaded via logo upload endpoint)"
            }
    
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


@router.post("/branding/upload-logo")
async def upload_logo(
    file: UploadFile = File(...),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Upload logo image for branding"""
    # Check if user is admin
    if current_user.role != "admin":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only admins can upload logos"
        )
    
    # Validate file type (images only)
    allowed_extensions = {'.png', '.jpg', '.jpeg', '.gif', '.svg', '.webp'}
    file_ext = Path(file.filename).suffix.lower() if file.filename else ''
    
    if file_ext not in allowed_extensions:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid file type. Allowed: {', '.join(allowed_extensions)}"
        )
    
    # Validate file size (max 5MB)
    content = await file.read()
    if len(content) > 5 * 1024 * 1024:
        raise HTTPException(
            status_code=400,
            detail="File size exceeds 5MB limit"
        )
    
    # Generate filename
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    safe_filename = f"logo_{timestamp}{file_ext}"
    logo_dir = _get_logo_dir()
    file_path = logo_dir / safe_filename
    
    # Save file
    try:
        async with aiofiles.open(file_path, 'wb') as f:
            await f.write(content)
        
        # Generate file URL
        file_url = f"/api/settings/branding/logo/{safe_filename}"
        
        # Save logo URL to settings
        await SettingsService.set_setting(
            db,
            "branding",
            "logo_url",
            file_url,
            encrypted=False,
            description="URL to the logo image file"
        )
        
        return {
            "success": True,
            "logo_url": file_url,
            "filename": safe_filename
        }
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Failed to save logo: {str(e)}"
        )


@router.get("/branding/public")
async def get_public_branding(
    db: AsyncSession = Depends(get_db)
    # No auth required - public branding info for login page
):
    """Get public branding settings (logo and system name) - no auth required"""
    branding_settings = await SettingsService.get_category_settings(db, "branding")
    
    # Apply defaults if not set
    system_name = "GayPHX Radio Traffic System"
    header_color = "#424242"
    logo_url = ""
    
    if branding_settings.get("system_name") and branding_settings["system_name"].get("value"):
        system_name = branding_settings["system_name"]["value"]
    if branding_settings.get("header_color") and branding_settings["header_color"].get("value"):
        header_color = branding_settings["header_color"]["value"]
    if branding_settings.get("logo_url") and branding_settings["logo_url"].get("value"):
        logo_url = branding_settings["logo_url"]["value"]
    
    return {
        "system_name": system_name,
        "header_color": header_color,
        "logo_url": logo_url
    }


@router.get("/branding/logo/{filename}")
async def get_logo(filename: str):
    """Serve logo file (public endpoint - no auth required)"""
    logo_dir = _get_logo_dir()
    file_path = logo_dir / filename
    
    if not file_path.exists() or not file_path.is_file():
        raise HTTPException(
            status_code=404,
            detail="Logo not found"
        )
    
    # Determine content type
    ext = file_path.suffix.lower()
    content_types = {
        '.png': 'image/png',
        '.jpg': 'image/jpeg',
        '.jpeg': 'image/jpeg',
        '.gif': 'image/gif',
        '.svg': 'image/svg+xml',
        '.webp': 'image/webp',
    }
    media_type = content_types.get(ext, 'image/png')
    
    return FileResponse(
        file_path,
        media_type=media_type,
        filename=filename
    )

