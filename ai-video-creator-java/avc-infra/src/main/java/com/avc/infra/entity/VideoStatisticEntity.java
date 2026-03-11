package com.avc.infra.entity;

import com.avc.common.enums.Platform;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@TableName("video_statistics")
@Getter
@Setter
@NoArgsConstructor
public class VideoStatisticEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String jobId;

    private String title;

    private String videoType;

    private int durationSeconds;

    private long fileSizeBytes;

    private Platform platform;

    private long viewCount;

    private long likeCount;

    private long shareCount;

    private long commentCount;

    private long generationTimeMs;

    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;
}
