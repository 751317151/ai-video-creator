package com.avc.common.dto.mq;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Enhanced MQ message: Java -> Python (video-task-submit).
 * Contains ALL configuration so Python Worker needs no local config.
 */
public record VideoTaskMessage(
        @JsonProperty("job_id") String jobId,
        String action,
        String topic,
        @JsonProperty("script_json") String scriptJson,
        @JsonProperty("video_type") String videoType,
        @JsonProperty("video_source") String videoSource,

        // TTS config
        String voice,
        @JsonProperty("tts_rate") String ttsRate,

        // Video rendering config
        @JsonProperty("video_width") int videoWidth,
        @JsonProperty("video_height") int videoHeight,
        @JsonProperty("video_fps") int videoFps,
        @JsonProperty("subtitle_font_size") int subtitleFontSize,
        @JsonProperty("subtitle_color") String subtitleColor,

        // Media source config
        @JsonProperty("pexels_api_key") String pexelsApiKey,
        @JsonProperty("ai_video_provider") String aiVideoProvider,
        @JsonProperty("ai_video_api_key") String aiVideoApiKey,

        // BGM config
        @JsonProperty("bgm_storage_path") String bgmStoragePath,
        @JsonProperty("bgm_volume") double bgmVolume,

        // Extra
        @JsonProperty("extra_requirements") String extraRequirements,
        @JsonProperty("template_id") String templateId
) {
    /**
     * Create a minimal cancel message.
     */
    public static VideoTaskMessage cancel(String jobId) {
        return new VideoTaskMessage(
                jobId, "CANCEL", null, null, null, null,
                null, null, 0, 0, 0, 0, null,
                null, null, null, null, 0.0, null, null
        );
    }
}
