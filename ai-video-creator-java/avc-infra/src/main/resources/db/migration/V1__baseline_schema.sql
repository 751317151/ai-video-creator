-- V1__baseline_schema.sql
-- Baseline migration: captures existing schema

CREATE TABLE IF NOT EXISTS jobs (
    id                  VARCHAR(40) PRIMARY KEY,
    status              VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    topic               TEXT NOT NULL,
    video_type          VARCHAR(30),
    video_source        VARCHAR(30),
    voice               VARCHAR(100),
    extra_requirements  TEXT,
    template_id         VARCHAR(64),
    bgm_track_id        VARCHAR(64),
    bgm_volume          DOUBLE PRECISION DEFAULT 0.3,
    progress            INTEGER DEFAULT 0,
    progress_message    VARCHAR(500),
    video_path          VARCHAR(1000),
    thumbnail_path      VARCHAR(1000),
    title               TEXT,
    description         TEXT,
    duration_seconds    INTEGER DEFAULT 0,
    error               TEXT,
    script_json         TEXT,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS script_knowledge (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(500) NOT NULL,
    video_type      VARCHAR(30),
    tags            TEXT,
    script_content  TEXT NOT NULL,
    view_count      INTEGER DEFAULT 0,
    like_count      INTEGER DEFAULT 0,
    job_id          VARCHAR(40),
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS moderation_log (
    id              BIGSERIAL PRIMARY KEY,
    verdict         VARCHAR(20) NOT NULL,
    confidence      DOUBLE PRECISION,
    flagged_issues  TEXT,
    suggestion      TEXT,
    checked_content TEXT NOT NULL,
    content_type    VARCHAR(20),
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
