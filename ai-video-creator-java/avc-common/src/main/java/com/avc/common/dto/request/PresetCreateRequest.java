package com.avc.common.dto.request;

import com.avc.common.enums.PresetCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PresetCreateRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must not exceed 100 characters")
        String name,

        @Size(max = 500, message = "description must not exceed 500 characters")
        String description,

        PresetCategory category,

        String videoType,
        String voice,
        String ttsRate,

        @Size(max = 2000, message = "extraRequirements must not exceed 2000 characters")
        String extraRequirements,

        Integer minDuration,
        Integer maxDuration,
        Integer subtitleFontSize,
        String subtitleColor,
        String defaultTags
) {}
