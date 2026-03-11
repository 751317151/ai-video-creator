package com.avc.common.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ModerationResult(
        boolean safe,
        @JsonProperty("risk_level") String riskLevel,
        List<String> issues,
        String suggestion
) {
}
