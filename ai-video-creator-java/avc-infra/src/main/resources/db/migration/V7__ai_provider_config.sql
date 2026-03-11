-- V7: AI provider configuration table
CREATE TABLE IF NOT EXISTS ai_provider_config (
    id              BIGSERIAL PRIMARY KEY,
    provider_name   VARCHAR(50) NOT NULL UNIQUE,
    api_key_encrypted TEXT,
    base_url        VARCHAR(500),
    model_name      VARCHAR(100),
    description     TEXT,
    enabled         BOOLEAN NOT NULL DEFAULT true,
    extra_config    TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_config_provider ON ai_provider_config (provider_name);
