package com.avc.infra.entity;

import com.avc.common.enums.PresetCategory;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@TableName("presets")
@Getter
@Setter
@NoArgsConstructor
public class PresetEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private PresetCategory category;

    private String videoType;

    private String voice;

    private String ttsRate;

    private String extraRequirements;

    private int minDuration;

    private int maxDuration;

    private int subtitleFontSize = 52;

    private String subtitleColor = "white";

    private String defaultTags;

    private boolean builtin = false;

    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
}
