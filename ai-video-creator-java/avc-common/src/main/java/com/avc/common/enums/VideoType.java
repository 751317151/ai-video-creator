package com.avc.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum VideoType {

    KNOWLEDGE("knowledge"),
    NEWS("news"),
    STORY("story"),
    TUTORIAL("tutorial"),
    PRODUCT("product");

    private final String value;

    VideoType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
