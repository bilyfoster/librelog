-- Add system_config table
CREATE TABLE IF NOT EXISTS system_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Basic System Info
    organization_name VARCHAR(255) DEFAULT 'GayPHX Music Platform',
    organization_description TEXT,
    contact_email VARCHAR(255) DEFAULT 'music@gayphx.com',
    support_email VARCHAR(255) DEFAULT 'support@gayphx.com',
    
    -- Branding
    logo_url VARCHAR(500),
    favicon_url VARCHAR(500),
    primary_color VARCHAR(7) DEFAULT '#667eea',
    secondary_color VARCHAR(7) DEFAULT '#764ba2',
    accent_color VARCHAR(7) DEFAULT '#f093fb',
    background_color VARCHAR(7) DEFAULT '#f8f9fa',
    text_color VARCHAR(7) DEFAULT '#333333',
    
    -- ISRC Configuration
    isrc_country_code VARCHAR(2) DEFAULT 'US',
    isrc_registrant_code VARCHAR(3) DEFAULT 'GPH',
    isrc_organization_name VARCHAR(255) DEFAULT 'GayPHX Music',
    
    -- Email Configuration
    smtp_host VARCHAR(255),
    smtp_port VARCHAR(10) DEFAULT '587',
    smtp_username VARCHAR(255),
    smtp_password VARCHAR(255),
    smtp_use_tls BOOLEAN DEFAULT TRUE,
    from_email VARCHAR(255) DEFAULT 'noreply@gayphx.com',
    from_name VARCHAR(255) DEFAULT 'GayPHX Music Platform',
    
    -- File Upload Settings
    max_file_size_mb VARCHAR(10) DEFAULT '150',
    allowed_file_types JSON DEFAULT '["mp3", "wav", "m4a", "flac"]',
    
    -- Audio Processing Settings
    target_lufs VARCHAR(10) DEFAULT '-14.0',
    max_true_peak VARCHAR(10) DEFAULT '-1.0',
    
    -- LibreTime Integration
    libretime_url VARCHAR(255),
    libretime_api_key VARCHAR(255),
    libretime_sync_enabled BOOLEAN DEFAULT FALSE,
    libretime_sync_interval VARCHAR(10) DEFAULT '15',
    
    -- Legal & Compliance
    terms_of_service_url VARCHAR(500),
    privacy_policy_url VARCHAR(500),
    copyright_notice VARCHAR(255) DEFAULT '© 2025 GayPHX Music Platform',
    
    -- Feature Flags
    enable_public_gallery BOOLEAN DEFAULT TRUE,
    enable_artist_registration BOOLEAN DEFAULT TRUE,
    enable_isrc_assignment BOOLEAN DEFAULT TRUE,
    enable_play_tracking BOOLEAN DEFAULT TRUE,
    enable_rights_management BOOLEAN DEFAULT TRUE,
    enable_commercial_use_tracking BOOLEAN DEFAULT TRUE,
    
    -- Social Media Links
    social_links JSON DEFAULT '{"website": "", "facebook": "", "twitter": "", "instagram": "", "youtube": "", "tiktok": ""}',
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_by UUID
);

-- Insert default configuration
INSERT INTO system_config (id) VALUES (gen_random_uuid())
ON CONFLICT (id) DO NOTHING;

