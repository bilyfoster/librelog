-- Initialize GayPHX Music Database
-- This script runs on PostgreSQL container startup

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Create enum types
DO $$ BEGIN
    CREATE TYPE submission_status AS ENUM ('pending', 'approved', 'rejected', 'needs_info', 'isrc_assigned');
    CREATE TYPE user_role AS ENUM ('admin', 'artist');
    CREATE TYPE file_status AS ENUM ('uploading', 'processing', 'ready', 'error');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Artists table
CREATE TABLE IF NOT EXISTS artists (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    pronouns VARCHAR(50),
    bio TEXT,
    social_links JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    is_active BOOLEAN DEFAULT TRUE
);

-- Admin users table
CREATE TABLE IF NOT EXISTS admin_users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role user_role DEFAULT 'admin',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_login TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT TRUE
);

-- Submissions table
CREATE TABLE IF NOT EXISTS submissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tracking_id VARCHAR(20) UNIQUE NOT NULL,
    artist_id UUID NOT NULL REFERENCES artists(id) ON DELETE CASCADE,
    song_title VARCHAR(255) NOT NULL,
    genre VARCHAR(100),
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_status file_status DEFAULT 'uploading',
    duration_seconds INTEGER,
    bitrate INTEGER,
    sample_rate INTEGER,
    channels INTEGER,
    lufs_reading DECIMAL(5,2),
    true_peak DECIMAL(5,2),
    status submission_status DEFAULT 'pending',
    isrc_requested BOOLEAN DEFAULT FALSE,
    radio_permission BOOLEAN DEFAULT FALSE,
    public_display BOOLEAN DEFAULT FALSE,
    rights_attestation BOOLEAN DEFAULT FALSE,
    pro_info JSONB DEFAULT '{}',
    admin_notes TEXT,
    submitted_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    reviewed_by UUID REFERENCES admin_users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ISRCs table
CREATE TABLE IF NOT EXISTS isrcs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    submission_id UUID NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
    isrc_code VARCHAR(12) UNIQUE NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    registrant_code VARCHAR(3) NOT NULL,
    year INTEGER NOT NULL,
    sequence_number INTEGER NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    assigned_by UUID REFERENCES admin_users(id),
    certificate_path VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Collaborators table
CREATE TABLE IF NOT EXISTS collaborators (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    submission_id UUID NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(100) NOT NULL, -- 'writer', 'producer', 'performer', etc.
    email VARCHAR(255),
    pro_affiliation VARCHAR(50), -- 'ASCAP', 'BMI', 'SESAC', etc.
    split_percentage DECIMAL(5,2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Audit logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    table_name VARCHAR(50) NOT NULL,
    record_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL, -- 'INSERT', 'UPDATE', 'DELETE'
    old_values JSONB,
    new_values JSONB,
    user_id UUID,
    user_type user_role,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ISRC sequence counter table
CREATE TABLE IF NOT EXISTS isrc_sequence (
    id SERIAL PRIMARY KEY,
    year INTEGER NOT NULL,
    current_sequence INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(year)
);

-- Magic link tokens table
CREATE TABLE IF NOT EXISTS magic_link_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_artists_email ON artists(email);
CREATE INDEX IF NOT EXISTS idx_artists_name ON artists(name);
CREATE INDEX IF NOT EXISTS idx_submissions_artist_id ON submissions(artist_id);
CREATE INDEX IF NOT EXISTS idx_submissions_status ON submissions(status);
CREATE INDEX IF NOT EXISTS idx_submissions_tracking_id ON submissions(tracking_id);
CREATE INDEX IF NOT EXISTS idx_submissions_submitted_at ON submissions(submitted_at);
CREATE INDEX IF NOT EXISTS idx_submissions_isrc_requested ON submissions(isrc_requested);
CREATE INDEX IF NOT EXISTS idx_isrcs_submission_id ON isrcs(submission_id);
CREATE INDEX IF NOT EXISTS idx_isrcs_code ON isrcs(isrc_code);
CREATE INDEX IF NOT EXISTS idx_collaborators_submission_id ON collaborators(submission_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_table_record ON audit_logs(table_name, record_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_magic_tokens_email ON magic_link_tokens(email);
CREATE INDEX IF NOT EXISTS idx_magic_tokens_token ON magic_link_tokens(token);
CREATE INDEX IF NOT EXISTS idx_magic_tokens_expires_at ON magic_link_tokens(expires_at);

-- Create updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Add updated_at triggers
CREATE TRIGGER update_artists_updated_at BEFORE UPDATE ON artists FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_submissions_updated_at BEFORE UPDATE ON submissions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_isrc_sequence_updated_at BEFORE UPDATE ON isrc_sequence FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert initial ISRC sequence for current year
INSERT INTO isrc_sequence (year, current_sequence) 
VALUES (EXTRACT(YEAR FROM NOW())::INTEGER, 0)
ON CONFLICT (year) DO NOTHING;

-- Create a function to generate tracking IDs
CREATE OR REPLACE FUNCTION generate_tracking_id()
RETURNS VARCHAR(20) AS $$
DECLARE
    new_tracking_id VARCHAR(20);
    exists_count INTEGER;
BEGIN
    LOOP
        -- Generate a random 8-character alphanumeric string
        new_tracking_id := UPPER(SUBSTRING(MD5(RANDOM()::TEXT) FROM 1 FOR 8));
        
        -- Check if it already exists
        SELECT COUNT(*) INTO exists_count FROM submissions WHERE submissions.tracking_id = new_tracking_id;
        
        -- If it doesn't exist, we're good
        IF exists_count = 0 THEN
            EXIT;
        END IF;
    END LOOP;
    
    RETURN new_tracking_id;
END;
$$ LANGUAGE plpgsql;
