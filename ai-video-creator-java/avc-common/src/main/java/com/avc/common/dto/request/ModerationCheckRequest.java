package com.avc.common.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModerationCheckRequest(
        @NotBlank(message = "content is required")
        @Size(max = 20000, message = "content must not exceed 20000 characters")
        String content,
        @JsonProperty("content_type")
        @Size(max = 30, message = "contentType must not exceed 30 characters")
        String contentType
) {
    public ModerationCheckRequest {
        if (contentType == null) {
            contentType = "script";
        }
    }
}
