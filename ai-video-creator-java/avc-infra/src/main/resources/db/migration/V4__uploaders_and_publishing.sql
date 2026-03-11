-- V4: Platform credentials, publish records, scheduled tasks
CREATE TABLE IF NOT EXISTS platform_credentials (
    id              BIGSERIAL PRIMARY KEY,
    platform        VARCHAR(30) NOT NULL,
    credential_data TEXT NOT NULL,
    enabled         BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS publish_records (
    id              BIGSERIAL PRIMARY KEY,
    job_id          VARCHAR(40) NOT NULL,
    platform        VARCHAR(30) NOT NULL,
    platform_video_id VARCHAR(200),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error           TEXT,
    published_at    TIMESTAMP,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS scheduled_tasks (
    id              BIGSERIAL PRIMARY KEY,
    job_id          VARCHAR(40) NOT NULL,
    platform        VARCHAR(30) NOT NULL,
    scheduled_time  TIMESTAMP NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error           TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_creds_platform ON platform_credentials (platform);
CREATE INDEX IF NOT EXISTS idx_publish_job_id ON publish_records (job_id);
CREATE INDEX IF NOT EXISTS idx_publish_platform ON publish_records (platform);
CREATE INDEX IF NOT EXISTS idx_publish_status ON publish_records (status);
CREATE INDEX IF NOT EXISTS idx_sched_status ON scheduled_tasks (status);
CREATE INDEX IF NOT EXISTS idx_sched_time ON scheduled_tasks (scheduled_time);
