package com.avc.infra.entity;

import com.avc.common.enums.Platform;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@TableName("publish_records")
@Getter
@Setter
@NoArgsConstructor
public class PublishRecordEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String jobId;

    private Platform platform;

    private String platformVideoId;

    private String status = "PENDING";

    private String error;

    private Instant publishedAt;

    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;
}
