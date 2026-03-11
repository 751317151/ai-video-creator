package com.avc.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum VideoSource {

    IMAGE("image"),
    PEXELS_VIDEO("pexels_video"),
    AI_VIDEO("ai_video");

    private final String value;

    VideoSource(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
