from sqlalchemy import Column, String, Text, Boolean, DateTime, JSON
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
from app.core.database import Base
import uuid


class SystemConfig(Base):
    __tablename__ = "system_config"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    
    # Basic System Info
    organization_name = Column(String(255), default="GayPHX Music Platform")
    organization_description = Column(Text)
    contact_email = Column(String(255), default="music@gayphx.com")
    support_email = Column(String(255), default="support@gayphx.com")
    
    # Branding
    logo_url = Column(String(500))
    favicon_url = Column(String(500))
    primary_color = Column(String(7), default="#667eea")  # Hex color
    secondary_color = Column(String(7), default="#764ba2")  # Hex color
    accent_color = Column(String(7), default="#f093fb")  # Hex color
    background_color = Column(String(7), default="#f8f9fa")  # Hex color
    text_color = Column(String(7), default="#333333")  # Hex color
    
    # ISRC Configuration
    isrc_country_code = Column(String(2), default="US")
    isrc_registrant_code = Column(String(3), default="GPH")  # 3 characters max
    isrc_organization_name = Column(String(255), default="GayPHX Music")
    
    # Email Configuration
    smtp_host = Column(String(255))
    smtp_port = Column(String(10), default="587")
    smtp_username = Column(String(255))
    smtp_password = Column(String(255))
    smtp_use_tls = Column(Boolean, default=True)
    from_email = Column(String(255), default="noreply@gayphx.com")
    from_name = Column(String(255), default="GayPHX Music Platform")
    
    # File Upload Settings
    max_file_size_mb = Column(String(10), default="150")
    allowed_file_types = Column(JSON, default=["mp3", "wav", "m4a", "flac"])
    
    # Audio Processing Settings
    target_lufs = Column(String(10), default="-14.0")
    max_true_peak = Column(String(10), default="-1.0")
    
    # LibreTime Integration
    libretime_url = Column(String(255))
    libretime_api_key = Column(String(255))
    libretime_sync_enabled = Column(Boolean, default=False)
    libretime_sync_interval = Column(String(10), default="15")  # minutes
    
    # Legal & Compliance
    terms_of_service_url = Column(String(500))
    privacy_policy_url = Column(String(500))
    copyright_notice = Column(String(255), default="© 2025 GayPHX Music Platform")
    
    # Feature Flags
    enable_public_gallery = Column(Boolean, default=True)
    enable_artist_registration = Column(Boolean, default=True)
    enable_isrc_assignment = Column(Boolean, default=True)
    enable_play_tracking = Column(Boolean, default=True)
    enable_rights_management = Column(Boolean, default=True)
    enable_commercial_use_tracking = Column(Boolean, default=True)
    
    # Social Media Links
    social_links = Column(JSON, default={
        "website": "",
        "facebook": "",
        "twitter": "",
        "instagram": "",
        "youtube": "",
        "tiktok": ""
    })
    
    # Timestamps
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    updated_by = Column(UUID(as_uuid=True))  # Admin who last updated

