package com.avc.infra.entity;

import com.avc.common.enums.JobStatus;
import com.avc.common.enums.VideoSource;
import com.avc.common.enums.VideoType;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@TableName("jobs")
@Getter
@Setter
@NoArgsConstructor
public class JobEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    private JobStatus status = JobStatus.QUEUED;

    private String topic;

    private VideoType videoType;

    private VideoSource videoSource;

    private String voice;

    private String extraRequirements;

    private String templateId;

    private String bgmTrackId;

    private double bgmVolume = 0.3;

    private int progress;

    private String progressMessage;

    private String videoPath;

    private String thumbnailPath;

    private String title;

    private String description;

    private int durationSeconds;

    private String error;

    private String scriptJson;

    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
}
