-- V6: Music library table
CREATE TABLE IF NOT EXISTS music_tracks (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    artist          VARCHAR(200),
    category        VARCHAR(30),
    mood            VARCHAR(30),
    duration_seconds INT DEFAULT 0,
    storage_key     VARCHAR(500) NOT NULL,
    file_size       BIGINT DEFAULT 0,
    use_count       INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_music_category ON music_tracks (category);
CREATE INDEX IF NOT EXISTS idx_music_mood ON music_tracks (mood);
