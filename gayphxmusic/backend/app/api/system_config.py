from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.models.system_config import SystemConfig
from pydantic import BaseModel, validator
from typing import Optional, List, Dict, Any
import re
import uuid

router = APIRouter()


class SystemConfigUpdate(BaseModel):
    # Basic System Info
    organization_name: Optional[str] = None
    organization_description: Optional[str] = None
    contact_email: Optional[str] = None
    support_email: Optional[str] = None
    
    # Branding
    logo_url: Optional[str] = None
    favicon_url: Optional[str] = None
    primary_color: Optional[str] = None
    secondary_color: Optional[str] = None
    accent_color: Optional[str] = None
    background_color: Optional[str] = None
    text_color: Optional[str] = None
    
    # ISRC Configuration
    isrc_country_code: Optional[str] = None
    isrc_registrant_code: Optional[str] = None
    isrc_organization_name: Optional[str] = None
    
    # Email Configuration
    smtp_host: Optional[str] = None
    smtp_port: Optional[str] = None
    smtp_username: Optional[str] = None
    smtp_password: Optional[str] = None
    smtp_use_tls: Optional[bool] = None
    from_email: Optional[str] = None
    from_name: Optional[str] = None
    
    # File Upload Settings
    max_file_size_mb: Optional[str] = None
    allowed_file_types: Optional[List[str]] = None
    
    # Audio Processing Settings
    target_lufs: Optional[str] = None
    max_true_peak: Optional[str] = None
    
    # LibreTime Integration
    libretime_url: Optional[str] = None
    libretime_api_key: Optional[str] = None
    libretime_sync_enabled: Optional[bool] = None
    libretime_sync_interval: Optional[str] = None
    
    # Legal & Compliance
    terms_of_service_url: Optional[str] = None
    privacy_policy_url: Optional[str] = None
    copyright_notice: Optional[str] = None
    
    # Feature Flags
    enable_public_gallery: Optional[bool] = None
    enable_artist_registration: Optional[bool] = None
    enable_isrc_assignment: Optional[bool] = None
    enable_play_tracking: Optional[bool] = None
    enable_rights_management: Optional[bool] = None
    enable_commercial_use_tracking: Optional[bool] = None
    
    # Social Media Links
    social_links: Optional[Dict[str, str]] = None

    @validator('primary_color', 'secondary_color', 'accent_color', 'background_color', 'text_color')
    def validate_hex_color(cls, v):
        if v is not None:
            if not re.match(r'^#[0-9A-Fa-f]{6}$', v):
                raise ValueError('Color must be a valid hex color (e.g., #FF0000)')
        return v

    @validator('isrc_registrant_code')
    def validate_isrc_registrant_code(cls, v):
        if v is not None:
            if len(v) != 3 or not v.isalpha():
                raise ValueError('ISRC registrant code must be exactly 3 letters')
        return v

    @validator('isrc_country_code')
    def validate_isrc_country_code(cls, v):
        if v is not None:
            if len(v) != 2 or not v.isalpha():
                raise ValueError('ISRC country code must be exactly 2 letters')
        return v

    @validator('contact_email', 'support_email', 'from_email')
    def validate_email(cls, v):
        if v is not None:
            if not re.match(r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$', v):
                raise ValueError('Invalid email format')
        return v


class SystemConfigResponse(BaseModel):
    id: str
    organization_name: str
    organization_description: Optional[str]
    contact_email: str
    support_email: str
    logo_url: Optional[str]
    favicon_url: Optional[str]
    primary_color: str
    secondary_color: str
    accent_color: str
    background_color: str
    text_color: str
    isrc_country_code: str
    isrc_registrant_code: str
    isrc_organization_name: str
    smtp_host: Optional[str]
    smtp_port: str
    smtp_username: Optional[str]
    smtp_password: Optional[str]
    smtp_use_tls: bool
    from_email: str
    from_name: str
    max_file_size_mb: str
    allowed_file_types: List[str]
    target_lufs: str
    max_true_peak: str
    libretime_url: Optional[str]
    libretime_api_key: Optional[str]
    libretime_sync_enabled: bool
    libretime_sync_interval: str
    terms_of_service_url: Optional[str]
    privacy_policy_url: Optional[str]
    copyright_notice: str
    enable_public_gallery: bool
    enable_artist_registration: bool
    enable_isrc_assignment: bool
    enable_play_tracking: bool
    enable_rights_management: bool
    enable_commercial_use_tracking: bool
    social_links: Dict[str, str]
    created_at: str
    updated_at: str

    class Config:
        from_attributes = True

    @classmethod
    def from_orm(cls, obj):
        return cls(
            id=str(obj.id),
            organization_name=obj.organization_name,
            organization_description=obj.organization_description,
            contact_email=obj.contact_email,
            support_email=obj.support_email,
            logo_url=obj.logo_url,
            favicon_url=obj.favicon_url,
            primary_color=obj.primary_color,
            secondary_color=obj.secondary_color,
            accent_color=obj.accent_color,
            background_color=obj.background_color,
            text_color=obj.text_color,
            isrc_country_code=obj.isrc_country_code,
            isrc_registrant_code=obj.isrc_registrant_code,
            isrc_organization_name=obj.isrc_organization_name,
            smtp_host=obj.smtp_host,
            smtp_port=obj.smtp_port,
            smtp_username=obj.smtp_username,
            smtp_password=obj.smtp_password,
            smtp_use_tls=obj.smtp_use_tls,
            from_email=obj.from_email,
            from_name=obj.from_name,
            max_file_size_mb=obj.max_file_size_mb,
            allowed_file_types=obj.allowed_file_types or [],
            target_lufs=obj.target_lufs,
            max_true_peak=obj.max_true_peak,
            libretime_url=obj.libretime_url,
            libretime_api_key=obj.libretime_api_key,
            libretime_sync_enabled=obj.libretime_sync_enabled,
            libretime_sync_interval=obj.libretime_sync_interval,
            terms_of_service_url=obj.terms_of_service_url,
            privacy_policy_url=obj.privacy_policy_url,
            copyright_notice=obj.copyright_notice,
            enable_public_gallery=obj.enable_public_gallery,
            enable_artist_registration=obj.enable_artist_registration,
            enable_isrc_assignment=obj.enable_isrc_assignment,
            enable_play_tracking=obj.enable_play_tracking,
            enable_rights_management=obj.enable_rights_management,
            enable_commercial_use_tracking=obj.enable_commercial_use_tracking,
            social_links=obj.social_links or {},
            created_at=obj.created_at.isoformat(),
            updated_at=obj.updated_at.isoformat()
        )


@router.get("/")
async def get_system_config(db: Session = Depends(get_db)):
    """Get current system configuration"""
    config = db.query(SystemConfig).first()
    
    if not config:
        # Create default configuration
        config = SystemConfig()
        db.add(config)
        db.commit()
        db.refresh(config)
    
    return {
        "id": str(config.id),
        "organization_name": config.organization_name,
        "organization_description": config.organization_description,
        "contact_email": config.contact_email,
        "support_email": config.support_email,
        "logo_url": config.logo_url,
        "favicon_url": config.favicon_url,
        "primary_color": config.primary_color,
        "secondary_color": config.secondary_color,
        "accent_color": config.accent_color,
        "background_color": config.background_color,
        "text_color": config.text_color,
        "isrc_country_code": config.isrc_country_code,
        "isrc_registrant_code": config.isrc_registrant_code,
        "isrc_organization_name": config.isrc_organization_name,
        "smtp_host": config.smtp_host,
        "smtp_port": config.smtp_port,
        "smtp_username": config.smtp_username,
        "smtp_password": config.smtp_password,
        "smtp_use_tls": config.smtp_use_tls,
        "from_email": config.from_email,
        "from_name": config.from_name,
        "max_file_size_mb": config.max_file_size_mb,
        "allowed_file_types": config.allowed_file_types or [],
        "target_lufs": config.target_lufs,
        "max_true_peak": config.max_true_peak,
        "libretime_url": config.libretime_url,
        "libretime_api_key": config.libretime_api_key,
        "libretime_sync_enabled": config.libretime_sync_enabled,
        "libretime_sync_interval": config.libretime_sync_interval,
        "terms_of_service_url": config.terms_of_service_url,
        "privacy_policy_url": config.privacy_policy_url,
        "copyright_notice": config.copyright_notice,
        "enable_public_gallery": config.enable_public_gallery,
        "enable_artist_registration": config.enable_artist_registration,
        "enable_isrc_assignment": config.enable_isrc_assignment,
        "enable_play_tracking": config.enable_play_tracking,
        "enable_rights_management": config.enable_rights_management,
        "enable_commercial_use_tracking": config.enable_commercial_use_tracking,
        "social_links": config.social_links or {},
        "created_at": config.created_at.isoformat(),
        "updated_at": config.updated_at.isoformat()
    }


@router.put("/")
async def update_system_config(
    config_update: SystemConfigUpdate,
    admin_id: str = "f49dc2cf-69f7-4119-9a00-3d7046293321",  # TODO: Get from JWT token
    db: Session = Depends(get_db)
):
    """Update system configuration"""
    config = db.query(SystemConfig).first()
    
    if not config:
        config = SystemConfig()
        db.add(config)
    
    # Update fields that are provided
    update_data = config_update.dict(exclude_unset=True)
    
    for field, value in update_data.items():
        if hasattr(config, field):
            setattr(config, field, value)
    
    # Update metadata
    config.updated_by = admin_id
    
    db.commit()
    db.refresh(config)
    
    return {
        "id": str(config.id),
        "organization_name": config.organization_name,
        "organization_description": config.organization_description,
        "contact_email": config.contact_email,
        "support_email": config.support_email,
        "logo_url": config.logo_url,
        "favicon_url": config.favicon_url,
        "primary_color": config.primary_color,
        "secondary_color": config.secondary_color,
        "accent_color": config.accent_color,
        "background_color": config.background_color,
        "text_color": config.text_color,
        "isrc_country_code": config.isrc_country_code,
        "isrc_registrant_code": config.isrc_registrant_code,
        "isrc_organization_name": config.isrc_organization_name,
        "smtp_host": config.smtp_host,
        "smtp_port": config.smtp_port,
        "smtp_username": config.smtp_username,
        "smtp_password": config.smtp_password,
        "smtp_use_tls": config.smtp_use_tls,
        "from_email": config.from_email,
        "from_name": config.from_name,
        "max_file_size_mb": config.max_file_size_mb,
        "allowed_file_types": config.allowed_file_types or [],
        "target_lufs": config.target_lufs,
        "max_true_peak": config.max_true_peak,
        "libretime_url": config.libretime_url,
        "libretime_api_key": config.libretime_api_key,
        "libretime_sync_enabled": config.libretime_sync_enabled,
        "libretime_sync_interval": config.libretime_sync_interval,
        "terms_of_service_url": config.terms_of_service_url,
        "privacy_policy_url": config.privacy_policy_url,
        "copyright_notice": config.copyright_notice,
        "enable_public_gallery": config.enable_public_gallery,
        "enable_artist_registration": config.enable_artist_registration,
        "enable_isrc_assignment": config.enable_isrc_assignment,
        "enable_play_tracking": config.enable_play_tracking,
        "enable_rights_management": config.enable_rights_management,
        "enable_commercial_use_tracking": config.enable_commercial_use_tracking,
        "social_links": config.social_links or {},
        "created_at": config.created_at.isoformat(),
        "updated_at": config.updated_at.isoformat()
    }


@router.get("/branding")
async def get_branding_config(db: Session = Depends(get_db)):
    """Get branding configuration for frontend"""
    config = db.query(SystemConfig).first()
    
    if not config:
        # Return default branding
        return {
            "organization_name": "GayPHX Music Platform",
            "logo_url": None,
            "favicon_url": None,
            "primary_color": "#667eea",
            "secondary_color": "#764ba2",
            "accent_color": "#f093fb",
            "background_color": "#f8f9fa",
            "text_color": "#333333",
            "social_links": {}
        }
    
    return {
        "organization_name": config.organization_name,
        "logo_url": config.logo_url,
        "favicon_url": config.favicon_url,
        "primary_color": config.primary_color,
        "secondary_color": config.secondary_color,
        "accent_color": config.accent_color,
        "background_color": config.background_color,
        "text_color": config.text_color,
        "social_links": config.social_links or {}
    }


@router.get("/features")
async def get_feature_flags(db: Session = Depends(get_db)):
    """Get feature flags for frontend"""
    config = db.query(SystemConfig).first()
    
    if not config:
        # Return default feature flags
        return {
            "enable_public_gallery": True,
            "enable_artist_registration": True,
            "enable_isrc_assignment": True,
            "enable_play_tracking": True,
            "enable_rights_management": True,
            "enable_commercial_use_tracking": True
        }
    
    return {
        "enable_public_gallery": config.enable_public_gallery,
        "enable_artist_registration": config.enable_artist_registration,
        "enable_isrc_assignment": config.enable_isrc_assignment,
        "enable_play_tracking": config.enable_play_tracking,
        "enable_rights_management": config.enable_rights_management,
        "enable_commercial_use_tracking": config.enable_commercial_use_tracking
    }


@router.post("/reset")
async def reset_system_config(
    admin_id: str = "f49dc2cf-69f7-4119-9a00-3d7046293321",  # TODO: Get from JWT token
    db: Session = Depends(get_db)
):
    """Reset system configuration to defaults"""
    config = db.query(SystemConfig).first()
    
    if config:
        db.delete(config)
    
    # Create new default configuration
    config = SystemConfig()
    config.updated_by = admin_id
    db.add(config)
    db.commit()
    db.refresh(config)
    
    return {"message": "System configuration reset to defaults", "config": config}
