from pydantic_settings import BaseSettings
from typing import Optional
import os


class Settings(BaseSettings):
    # Database
    database_url: str = "postgresql://gayphx:gayphx_secure_password@localhost:5432/gayphx_music"
    
    # MinIO/S3 Storage
    minio_endpoint: str = "host.docker.internal:9002"
    minio_access_key: str = "gayphx"
    minio_secret_key: str = "gayphx_secure_password"
    minio_bucket: str = "gayphx-music"
    minio_secure: bool = False
    
    # Email
    smtp_url: str = "smtp://mailhog:1025"
    
    # ISRC Configuration
    isrc_country: str = "US"
    isrc_registrant: str = "GPH"  # 3 characters - MUST be registered with US ISRC
    
    # Security
    jwt_secret: str = "your-super-secret-jwt-key-change-in-production"
    jwt_algorithm: str = "HS256"
    jwt_expire_minutes: int = 60 * 24 * 7  # 7 days
    
    # Magic Link
    magic_link_expire_minutes: int = 15
    
    # Frontend
    frontend_url: str = "http://localhost:3000"
    
    # File Upload
    max_file_size_mb: int = 150
    allowed_audio_types: list = [".mp3"]  # MP3 only for studio compatibility
    
    # Audio Processing
    target_lufs: float = -14.0
    max_true_peak: float = -1.0
    
    class Config:
        env_file = ".env"
        case_sensitive = False


settings = Settings()
