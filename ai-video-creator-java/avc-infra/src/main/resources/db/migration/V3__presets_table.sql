-- V3: Presets table with built-in seed data
CREATE TABLE IF NOT EXISTS presets (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    category        VARCHAR(30),
    video_type      VARCHAR(30),
    voice           VARCHAR(100),
    tts_rate        VARCHAR(20),
    extra_requirements TEXT,
    min_duration    INT DEFAULT 60,
    max_duration    INT DEFAULT 90,
    subtitle_font_size INT DEFAULT 52,
    subtitle_color  VARCHAR(20) DEFAULT 'white',
    default_tags    TEXT,
    builtin         BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_presets_category ON presets (category);
CREATE INDEX IF NOT EXISTS idx_presets_builtin ON presets (builtin);

-- Seed: 6 built-in presets
INSERT INTO presets (name, description, category, video_type, voice, tts_rate, extra_requirements,
                     min_duration, max_duration, subtitle_font_size, subtitle_color, default_tags, builtin)
VALUES
    ('抖音爆款', '适合抖音的快节奏、高完播率内容', 'VIRAL', 'news', 'zh-CN-XiaoyiNeural', '+15%',
     '节奏快、开头吸引人、每段15字以内', 30, 45, 48, 'white',
     '推荐,必看,收藏,分享', true),

    ('B站科普', '适合B站的深度科普内容', 'KNOWLEDGE', 'knowledge', 'zh-CN-YunxiNeural', '+0%',
     '内容详实、逻辑清晰、适合深度学习', 90, 120, 56, 'white',
     '科普,知识,学习,涨知识,干货', true),

    ('故事讲述', '适合情感故事、悬疑故事', 'STORY', 'story', 'zh-CN-XiaoxiaoNeural', '-10%',
     '故事性强、有反转、情绪起伏明显', 90, 150, 54, 'white',
     '故事,真实经历,奇闻,情感', true),

    ('产品推广', '带货营销视频', 'MARKETING', 'product', 'zh-CN-XiaoyiNeural', '+10%',
     '突出卖点、带具体数据、CTA明确', 30, 60, 52, 'white',
     '好物推荐,种草,好物,必买,评测', true),

    ('快速新闻', '热点新闻速报', 'NEWS', 'news', 'zh-CN-YunyangNeural', '+20%',
     '新闻语言、信息密度高、客观中立', 30, 45, 50, '#ffcc00',
     '新闻,热点,资讯,时事', true),

    ('教程教学', '教程类内容', 'EDUCATION', 'knowledge', 'zh-CN-YunxiNeural', '+0%',
     '步骤清晰、语言简洁、适合学习', 60, 90, 56, 'white',
     '教程,教学,学习,指南', true);
