package com.avc.common.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @JsonProperty("session_id") String sessionId,
        @NotBlank(message = "message is required")
        @Size(max = 5000, message = "message must not exceed 5000 characters")
        String message
) {
}
