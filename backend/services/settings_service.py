"""
Settings service for managing application configuration
"""

import os
import base64
from typing import Dict, Any, Optional
from cryptography.fernet import Fernet
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
import structlog

from backend.models.settings import Setting
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, update, delete

logger = structlog.get_logger()


class SettingsService:
    """Service for managing application settings"""

    # Encryption key derived from environment or default
    @staticmethod
    def _get_encryption_key() -> bytes:
        """Get or generate encryption key"""
        key_seed = os.getenv("SETTINGS_ENCRYPTION_KEY", "librelog-default-key-change-in-production")
        kdf = PBKDF2HMAC(
            algorithm=hashes.SHA256(),
            length=32,
            salt=b'librelog_salt',
            iterations=100000,
        )
        key = base64.urlsafe_b64encode(kdf.derive(key_seed.encode()))
        return key

    @staticmethod
    def _encrypt_value(value: str) -> str:
        """Encrypt a value"""
        key = SettingsService._get_encryption_key()
        f = Fernet(key)
        encrypted = f.encrypt(value.encode())
        return encrypted.decode()

    @staticmethod
    def _decrypt_value(encrypted_value: str) -> str:
        """Decrypt a value"""
        key = SettingsService._get_encryption_key()
        f = Fernet(key)
        decrypted = f.decrypt(encrypted_value.encode())
        return decrypted.decode()

    @staticmethod
    async def get_setting(
        db: AsyncSession,
        category: str,
        key: str,
        default: Optional[str] = None
    ) -> Optional[str]:
        """Get a setting value, with fallback to environment variable or default"""
        # First check database
        result = await db.execute(
            select(Setting).where(
                Setting.category == category,
                Setting.key == key
            )
        )
        setting = result.scalar_one_or_none()
        
        if setting:
            if setting.encrypted:
                try:
                    return SettingsService._decrypt_value(setting.value or "")
                except Exception as e:
                    logger.warning("decrypt_failed", category=category, key=key, error=str(e))
                    return default
            return setting.value
        
        # Fallback to environment variable
        env_key = f"{category.upper()}_{key.upper()}".replace("-", "_")
        env_value = os.getenv(env_key)
        if env_value:
            return env_value
        
        return default

    @staticmethod
    async def set_setting(
        db: AsyncSession,
        category: str,
        key: str,
        value: str,
        encrypted: bool = False,
        description: Optional[str] = None
    ) -> Setting:
        """Set a setting value"""
        # Check if setting exists
        result = await db.execute(
            select(Setting).where(
                Setting.category == category,
                Setting.key == key
            )
        )
        setting = result.scalar_one_or_none()
        
        # Prepare value
        final_value = value
        if encrypted and value:
            final_value = SettingsService._encrypt_value(value)
        
        if setting:
            # Update existing
            setting.value = final_value
            setting.encrypted = encrypted
            if description:
                setting.description = description
        else:
            # Create new
            setting = Setting(
                category=category,
                key=key,
                value=final_value,
                encrypted=encrypted,
                description=description
            )
            db.add(setting)
        
        await db.commit()
        await db.refresh(setting)
        return setting

    @staticmethod
    async def get_category_settings(
        db: AsyncSession,
        category: str
    ) -> Dict[str, Any]:
        """Get all settings for a category"""
        result = await db.execute(
            select(Setting).where(Setting.category == category)
        )
        settings = result.scalars().all()
        
        settings_dict = {}
        for setting in settings:
            value = setting.value
            if setting.encrypted and value:
                try:
                    value = SettingsService._decrypt_value(value)
                except Exception as e:
                    logger.warning("decrypt_failed", category=category, key=setting.key, error=str(e))
                    value = None
            
            settings_dict[setting.key] = {
                "value": value,
                "encrypted": setting.encrypted,
                "description": setting.description,
            }
        
        return settings_dict

    @staticmethod
    async def update_category_settings(
        db: AsyncSession,
        category: str,
        settings_dict: Dict[str, Any]
    ) -> Dict[str, Setting]:
        """Update multiple settings for a category"""
        updated = {}
        
        for key, data in settings_dict.items():
            if isinstance(data, dict):
                value = data.get("value", "")
                encrypted = data.get("encrypted", False)
                description = data.get("description")
            else:
                value = str(data)
                encrypted = False
                description = None
            
            setting = await SettingsService.set_setting(
                db, category, key, value, encrypted, description
            )
            updated[key] = setting
        
        return updated

    @staticmethod
    async def delete_setting(
        db: AsyncSession,
        category: str,
        key: str
    ) -> bool:
        """Delete a setting"""
        result = await db.execute(
            select(Setting).where(
                Setting.category == category,
                Setting.key == key
            )
        )
        setting = result.scalar_one_or_none()
        
        if setting:
            await db.delete(setting)
            await db.commit()
            return True
        
        return False

    @staticmethod
    async def test_smtp_connection(
        host: str,
        port: int,
        username: str,
        password: str,
        use_tls: bool = True
    ) -> Dict[str, Any]:
        """Test SMTP connection"""
        try:
            import smtplib
            from email.mime.text import MIMEText
            
            server = smtplib.SMTP(host, port)
            if use_tls:
                server.starttls()
            server.login(username, password)
            server.quit()
            
            return {"success": True, "message": "SMTP connection successful"}
        except Exception as e:
            logger.error("smtp_test_failed", error=str(e), exc_info=True)
            return {"success": False, "message": str(e)}

    @staticmethod
    async def test_s3_connection(
        access_key_id: str,
        secret_access_key: str,
        bucket_name: str,
        region: str = "us-east-1"
    ) -> Dict[str, Any]:
        """Test S3 connection"""
        try:
            import boto3
            from botocore.exceptions import ClientError
            
            s3_client = boto3.client(
                's3',
                aws_access_key_id=access_key_id,
                aws_secret_access_key=secret_access_key,
                region_name=region
            )
            
            # Try to list bucket
            s3_client.head_bucket(Bucket=bucket_name)
            
            return {"success": True, "message": "S3 connection successful"}
        except ClientError as e:
            logger.error("s3_test_failed", error=str(e), exc_info=True)
            return {"success": False, "message": str(e)}
        except Exception as e:
            logger.error("s3_test_failed", error=str(e), exc_info=True)
            return {"success": False, "message": str(e)}

    @staticmethod
    async def test_backblaze_connection(
        application_key_id: str,
        application_key: str,
        bucket_name: str
    ) -> Dict[str, Any]:
        """Test Backblaze B2 connection"""
        try:
            import b2sdk.v1 as b2
            
            info = b2.InMemoryAccountInfo()
            b2_api = b2.B2Api(info)
            b2_api.authorize_account("production", application_key_id, application_key)
            
            bucket = b2_api.get_bucket_by_name(bucket_name)
            # Test by listing files (limit 1)
            list(bucket.ls(folder_to_list="", max_items=1))
            
            return {"success": True, "message": "Backblaze B2 connection successful"}
        except Exception as e:
            logger.error("b2_test_failed", error=str(e), exc_info=True)
            return {"success": False, "message": str(e)}

