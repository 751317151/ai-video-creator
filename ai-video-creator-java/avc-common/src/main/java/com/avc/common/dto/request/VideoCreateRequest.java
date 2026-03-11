package com.avc.common.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VideoCreateRequest(
        @NotBlank(message = "topic is required")
        @Size(max = 500, message = "topic must not exceed 500 characters")
        String topic,
        @JsonProperty("custom_script")
        @Size(max = 10000, message = "customScript must not exceed 10000 characters")
        String customScript,
        @JsonProperty("video_type")
        @Size(max = 30, message = "videoType must not exceed 30 characters")
        String videoType,
        @JsonProperty("video_source")
        @Size(max = 30, message = "videoSource must not exceed 30 characters")
        String videoSource,
        @JsonProperty("ai_video_provider")
        @Size(max = 30, message = "aiVideoProvider must not exceed 30 characters")
        String aiVideoProvider,
        @Size(max = 100, message = "voice must not exceed 100 characters")
        String voice,
        @JsonProperty("extra_requirements")
        @Size(max = 2000, message = "extraRequirements must not exceed 2000 characters")
        String extraRequirements,
        @JsonProperty("publish_douyin") boolean publishDouyin,
        @JsonProperty("publish_bilibili") boolean publishBilibili,
        @JsonProperty("template_id") String templateId,
        @JsonProperty("bgm_track_id") String bgmTrackId,
        @JsonProperty("bgm_volume")
        @Min(value = 0, message = "bgmVolume must be >= 0")
        @Max(value = 1, message = "bgmVolume must be <= 1")
        double bgmVolume,
        @JsonProperty("script_json")
        @Size(max = 50000, message = "scriptJson must not exceed 50000 characters")
        String scriptJson
) {

    public VideoCreateRequest {
        if (videoType == null) {
            videoType = "knowledge";
        }
        if (videoSource == null) {
            videoSource = "pexels_video";
        }
        if (bgmVolume <= 0) {
            bgmVolume = 0.3;
        }
    }
}
