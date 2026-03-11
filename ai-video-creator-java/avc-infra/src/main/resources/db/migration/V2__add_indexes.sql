-- V2__add_indexes.sql
-- Performance indexes for existing tables

CREATE INDEX IF NOT EXISTS idx_jobs_status ON jobs(status);
CREATE INDEX IF NOT EXISTS idx_jobs_created_at ON jobs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_jobs_status_created ON jobs(status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_moderation_verdict ON moderation_log(verdict);
CREATE INDEX IF NOT EXISTS idx_moderation_created ON moderation_log(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_knowledge_video_type ON script_knowledge(video_type);
CREATE INDEX IF NOT EXISTS idx_knowledge_job_id ON script_knowledge(job_id);
