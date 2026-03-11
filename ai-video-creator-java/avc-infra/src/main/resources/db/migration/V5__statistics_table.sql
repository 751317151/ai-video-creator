-- V5: Video statistics table
CREATE TABLE IF NOT EXISTS video_statistics (
    id                  BIGSERIAL PRIMARY KEY,
    job_id              VARCHAR(40) NOT NULL,
    title               VARCHAR(500),
    video_type          VARCHAR(30),
    duration_seconds    INT DEFAULT 0,
    file_size_bytes     BIGINT DEFAULT 0,
    platform            VARCHAR(30),
    view_count          BIGINT DEFAULT 0,
    like_count          BIGINT DEFAULT 0,
    share_count         BIGINT DEFAULT 0,
    comment_count       BIGINT DEFAULT 0,
    generation_time_ms  BIGINT DEFAULT 0,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_stats_job_id ON video_statistics (job_id);
CREATE INDEX IF NOT EXISTS idx_stats_created_at ON video_statistics (created_at);
CREATE INDEX IF NOT EXISTS idx_stats_platform ON video_statistics (platform);
