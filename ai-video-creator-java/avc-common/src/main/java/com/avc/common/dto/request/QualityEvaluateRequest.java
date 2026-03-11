package com.avc.common.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QualityEvaluateRequest(
        @JsonProperty("video_path")
        @NotBlank(message = "videoPath is required")
        String videoPath,
        @NotBlank(message = "title is required")
        @Size(max = 500, message = "title must not exceed 500 characters")
        String title,
        @Size(max = 5000, message = "description must not exceed 5000 characters")
        String description,
        @JsonProperty("duration_seconds")
        @Min(value = 1, message = "durationSeconds must be >= 1")
        int durationSeconds
) {
}
