package com.avc.common.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record QualityReport(
        Map<String, Integer> scores,
        String suggestion,
        @JsonProperty("ready_to_publish") boolean readyToPublish
) {
}
