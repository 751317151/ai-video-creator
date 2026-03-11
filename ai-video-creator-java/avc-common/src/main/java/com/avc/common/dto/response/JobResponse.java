package com.avc.common.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record JobResponse(
        @JsonProperty("job_id") String jobId,
        String status,
        String topic,
        @JsonProperty("video_type") String videoType,
        int progress,
        @JsonProperty("progress_message") String progressMessage,
        @JsonProperty("video_path") String videoPath,
        @JsonProperty("thumbnail_path") String thumbnailPath,
        String title,
        String description,
        @JsonProperty("duration_seconds") int durationSeconds,
        String error,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {
}
