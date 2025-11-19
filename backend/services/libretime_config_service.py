"""
Service for managing LibreTime integration configuration in LibreLog settings
"""

import os
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from backend.models.settings import Setting
import structlog

logger = structlog.get_logger()


class LibreTimeConfigService:
    """Service for managing LibreTime configuration in settings"""
    
    @staticmethod
    async def save_libretime_config(db: AsyncSession) -> dict:
        """
        Save LibreTime configuration to LibreLog settings
        
        Reads from environment variables and saves to settings table
        """
        # Try LIBRETIME_API_URL first, fallback to LIBRETIME_URL
        libretime_api_url = os.getenv("LIBRETIME_API_URL", "") or os.getenv("LIBRETIME_URL", "").rstrip("/api/v2").rstrip("/")
        libretime_api_key = os.getenv("LIBRETIME_API_KEY", "")
        libretime_public_url = os.getenv("LIBRETIME_PUBLIC_URL", "")
        
        saved_settings = []
        
        # Save API URL
        if libretime_api_url:
            setting = await db.execute(
                select(Setting).where(
                    and_(
                        Setting.category == "integrations",
                        Setting.key == "libretime_api_url"
                    )
                )
            )
            existing = setting.scalar_one_or_none()
            
            if existing:
                existing.value = libretime_api_url
                existing.encrypted = False
                existing.description = "LibreTime API base URL"
            else:
                existing = Setting(
                    category="integrations",
                    key="libretime_api_url",
                    value=libretime_api_url,
                    encrypted=False,
                    description="LibreTime API base URL"
                )
                db.add(existing)
            
            saved_settings.append("libretime_api_url")
        
        # Save API Key (encrypted)
        if libretime_api_key:
            setting = await db.execute(
                select(Setting).where(
                    and_(
                        Setting.category == "integrations",
                        Setting.key == "libretime_api_key"
                    )
                )
            )
            existing = setting.scalar_one_or_none()
            
            if existing:
                existing.value = libretime_api_key
                existing.encrypted = True
                existing.description = "LibreTime API authentication key"
            else:
                existing = Setting(
                    category="integrations",
                    key="libretime_api_key",
                    value=libretime_api_key,
                    encrypted=True,
                    description="LibreTime API authentication key"
                )
                db.add(existing)
            
            saved_settings.append("libretime_api_key")
        
        # Save Public URL
        if libretime_public_url:
            setting = await db.execute(
                select(Setting).where(
                    and_(
                        Setting.category == "integrations",
                        Setting.key == "libretime_public_url"
                    )
                )
            )
            existing = setting.scalar_one_or_none()
            
            if existing:
                existing.value = libretime_public_url
                existing.encrypted = False
                existing.description = "LibreTime public web interface URL"
            else:
                existing = Setting(
                    category="integrations",
                    key="libretime_public_url",
                    value=libretime_public_url,
                    encrypted=False,
                    description="LibreTime public web interface URL"
                )
                db.add(existing)
            
            saved_settings.append("libretime_public_url")
        
        await db.commit()
        
        logger.info(
            "LibreTime configuration saved to settings",
            saved_settings=saved_settings
        )
        
        return {
            "success": True,
            "saved_settings": saved_settings,
            "libretime_api_url": libretime_api_url,
            "libretime_public_url": libretime_public_url,
            "libretime_api_key_set": bool(libretime_api_key)
        }
    
    @staticmethod
    async def get_libretime_config(db: AsyncSession) -> dict:
        """Get LibreTime configuration from settings"""
        settings = await db.execute(
            select(Setting).where(
                and_(
                    Setting.category == "integrations",
                    Setting.key.in_([
                        "libretime_api_url",
                        "libretime_api_key",
                        "libretime_public_url"
                    ])
                )
            )
        )
        
        config = {}
        for setting in settings.scalars().all():
            if setting.key == "libretime_api_key" and setting.encrypted:
                # Mask the key for security
                value = setting.value
                if value and len(value) > 10:
                    config[setting.key] = value[:10] + "..." + value[-4:]
                else:
                    config[setting.key] = "***" if value else None
            else:
                config[setting.key] = setting.value
        
        return config

