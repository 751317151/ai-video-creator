package com.avc.common.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record VideoScriptDto(
        String title,
        String hook,
        List<ScriptSegment> segments,
        @JsonProperty("call_to_action") String callToAction,
        List<String> tags,
        @JsonProperty("duration_estimate") int durationEstimate,
        @JsonProperty("video_type") String videoType
) {

    public record ScriptSegment(
            String text,
            @JsonProperty("image_keyword") String imageKeyword,
            int duration
    ) {
    }
}
