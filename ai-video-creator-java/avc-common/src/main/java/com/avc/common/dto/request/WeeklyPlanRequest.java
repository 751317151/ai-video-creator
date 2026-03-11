package com.avc.common.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WeeklyPlanRequest(
        @NotBlank(message = "niche is required")
        @Size(max = 200, message = "niche must not exceed 200 characters")
        String niche,
        @JsonProperty("target_audience")
        @Size(max = 200, message = "targetAudience must not exceed 200 characters")
        String targetAudience,
        @JsonProperty("videos_per_day")
        @Min(value = 1, message = "videosPerDay must be >= 1")
        @Max(value = 20, message = "videosPerDay must be <= 20")
        int videosPerDay
) {
    public WeeklyPlanRequest {
        if (videosPerDay <= 0) {
            videosPerDay = 2;
        }
    }
}
