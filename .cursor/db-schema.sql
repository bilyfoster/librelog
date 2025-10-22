

-- =============================================================
-- LibreLog Database Schema
-- =============================================================

-- =========================
-- USERS
-- =========================
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) CHECK (role IN ('admin', 'producer', 'dj', 'sales')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- TRACKS
-- =========================
CREATE TABLE tracks (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    artist VARCHAR(255),
    album VARCHAR(255),
    type VARCHAR(10) CHECK (type IN ('MUS', 'ADV', 'PSA', 'LIN', 'INT', 'PRO', 'BED')),
    genre VARCHAR(100),
    duration INTEGER,
    filepath TEXT NOT NULL,
    last_played TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- CAMPAIGNS
-- =========================
CREATE TABLE campaigns (
    id SERIAL PRIMARY KEY,
    advertiser VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    priority INTEGER DEFAULT 1,
    file_url TEXT,
    active BOOLEAN DEFAULT TRUE
);

-- =========================
-- CLOCKS
-- =========================
CREATE TABLE clock_templates (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    json_layout JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- LOGS
-- =========================
CREATE TABLE daily_logs (
    id SERIAL PRIMARY KEY,
    date DATE NOT NULL,
    generated_by INTEGER REFERENCES users(id),
    json_data JSONB NOT NULL,
    published BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- VOICE TRACKS
-- =========================
CREATE TABLE voice_tracks (
    id SERIAL PRIMARY KEY,
    show_name VARCHAR(255),
    file_url TEXT NOT NULL,
    scheduled_time TIMESTAMP,
    uploaded_by INTEGER REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- PLAYBACK HISTORY
-- =========================
CREATE TABLE playback_history (
    id SERIAL PRIMARY KEY,
    track_id INTEGER REFERENCES tracks(id),
    log_id INTEGER REFERENCES daily_logs(id),
    played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    duration_played INTEGER
);

-- =========================
-- REPORTS
-- =========================
CREATE VIEW reconciliation_report AS
SELECT 
    dl.date,
    t.title AS track_title,
    t.type AS track_type,
    ph.played_at,
    ph.duration_played
FROM playback_history ph
JOIN daily_logs dl ON ph.log_id = dl.id
JOIN tracks t ON ph.track_id = t.id;

-- =========================
-- INDEXES
-- =========================
CREATE INDEX idx_tracks_type ON tracks(type);
CREATE INDEX idx_campaigns_active ON campaigns(active);
CREATE INDEX idx_logs_date ON daily_logs(date);
CREATE INDEX idx_playback_date ON playback_history(played_at);