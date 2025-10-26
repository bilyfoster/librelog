-- Create play tracking tables

-- Play logs table
CREATE TABLE IF NOT EXISTS play_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id UUID NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
    played_at TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_played INTEGER,
    play_type VARCHAR(20) NOT NULL,
    source VARCHAR(50) NOT NULL,
    libretime_play_id VARCHAR(100),
    libretime_show_id VARCHAR(100),
    libretime_show_name VARCHAR(255),
    dj_name VARCHAR(255),
    show_name VARCHAR(255),
    time_slot VARCHAR(50),
    extra_data JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for play_logs
CREATE INDEX IF NOT EXISTS idx_play_logs_submission_played_at ON play_logs(submission_id, played_at);
CREATE INDEX IF NOT EXISTS idx_play_logs_played_at_type ON play_logs(played_at, play_type);
CREATE INDEX IF NOT EXISTS idx_play_logs_libretime_id ON play_logs(libretime_play_id);

-- Play statistics table
CREATE TABLE IF NOT EXISTS play_statistics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id UUID UNIQUE NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
    total_plays INTEGER DEFAULT 0,
    radio_plays INTEGER DEFAULT 0,
    podcast_plays INTEGER DEFAULT 0,
    commercial_plays INTEGER DEFAULT 0,
    plays_this_week INTEGER DEFAULT 0,
    plays_this_month INTEGER DEFAULT 0,
    plays_this_year INTEGER DEFAULT 0,
    most_played_hour INTEGER,
    most_played_day INTEGER,
    last_played_at TIMESTAMP WITH TIME ZONE,
    last_played_by VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- LibreTime integration table
CREATE TABLE IF NOT EXISTS libretime_integration (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    libretime_url VARCHAR(255) NOT NULL,
    api_key VARCHAR(255) NOT NULL,
    last_sync_at TIMESTAMP WITH TIME ZONE,
    sync_status VARCHAR(20) DEFAULT 'active',
    sync_interval_minutes INTEGER DEFAULT 15,
    auto_sync_enabled BOOLEAN DEFAULT true,
    last_error TEXT,
    error_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create a function to update play statistics
CREATE OR REPLACE FUNCTION update_play_statistics()
RETURNS TRIGGER AS $$
BEGIN
    -- Update or create statistics for the submission
    INSERT INTO play_statistics (submission_id, total_plays, radio_plays, podcast_plays, commercial_plays, plays_this_week, plays_this_month, plays_this_year, most_played_hour, most_played_day, last_played_at, last_played_by)
    SELECT 
        NEW.submission_id,
        COUNT(*) as total_plays,
        COUNT(*) FILTER (WHERE play_type = 'radio') as radio_plays,
        COUNT(*) FILTER (WHERE play_type = 'podcast') as podcast_plays,
        COUNT(*) FILTER (WHERE play_type = 'commercial') as commercial_plays,
        COUNT(*) FILTER (WHERE played_at >= NOW() - INTERVAL '7 days') as plays_this_week,
        COUNT(*) FILTER (WHERE played_at >= NOW() - INTERVAL '30 days') as plays_this_month,
        COUNT(*) FILTER (WHERE played_at >= NOW() - INTERVAL '1 year') as plays_this_year,
        MODE() WITHIN GROUP (ORDER BY EXTRACT(hour FROM played_at)) as most_played_hour,
        MODE() WITHIN GROUP (ORDER BY EXTRACT(dow FROM played_at)) as most_played_day,
        MAX(played_at) as last_played_at,
        (SELECT dj_name FROM play_logs WHERE submission_id = NEW.submission_id ORDER BY played_at DESC LIMIT 1) as last_played_by
    FROM play_logs 
    WHERE submission_id = NEW.submission_id
    ON CONFLICT (submission_id) DO UPDATE SET
        total_plays = EXCLUDED.total_plays,
        radio_plays = EXCLUDED.radio_plays,
        podcast_plays = EXCLUDED.podcast_plays,
        commercial_plays = EXCLUDED.commercial_plays,
        plays_this_week = EXCLUDED.plays_this_week,
        plays_this_month = EXCLUDED.plays_this_month,
        plays_this_year = EXCLUDED.plays_this_year,
        most_played_hour = EXCLUDED.most_played_hour,
        most_played_day = EXCLUDED.most_played_day,
        last_played_at = EXCLUDED.last_played_at,
        last_played_by = EXCLUDED.last_played_by,
        updated_at = NOW();
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically update statistics when a play is logged
CREATE TRIGGER trigger_update_play_statistics
    AFTER INSERT ON play_logs
    FOR EACH ROW
    EXECUTE FUNCTION update_play_statistics();

