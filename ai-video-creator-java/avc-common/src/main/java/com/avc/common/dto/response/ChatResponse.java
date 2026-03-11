package com.avc.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatResponse(
        @JsonProperty("session_id") String sessionId,
        String reply,
        String error
) {
}
