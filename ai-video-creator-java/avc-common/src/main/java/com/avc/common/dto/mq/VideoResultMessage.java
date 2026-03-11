package com.avc.common.dto.mq;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Enhanced MQ message: Python -> Java (video-result).
 * Includes file size for statistics tracking.
 */
public record VideoResultMessage(
        @JsonProperty("job_id") String jobId,
        String status,
        @JsonProperty("video_path") String videoPath,
        @JsonProperty("thumbnail_path") String thumbnailPath,
        String title,
        String description,
        @JsonProperty("duration_seconds") int durationSeconds,
        @JsonProperty("file_size_bytes") long fileSizeBytes,
        @JsonProperty("generation_time_ms") long generationTimeMs,
        String error
) {}
