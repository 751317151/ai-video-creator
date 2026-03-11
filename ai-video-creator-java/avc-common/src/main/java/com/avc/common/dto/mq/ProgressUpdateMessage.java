package com.avc.common.dto.mq;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProgressUpdateMessage(
        @JsonProperty("job_id") String jobId,
        int percent,
        String message
) {}
