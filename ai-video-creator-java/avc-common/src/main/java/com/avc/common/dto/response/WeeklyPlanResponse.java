package com.avc.common.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record WeeklyPlanResponse(
        List<DayPlan> days,
        String summary
) {

    public record DayPlan(
            String day,
            List<VideoSlot> videos
    ) {
    }

    public record VideoSlot(
            String topic,
            @JsonProperty("video_type") String videoType,
            @JsonProperty("publish_time") String publishTime,
            String description
    ) {
    }
}
