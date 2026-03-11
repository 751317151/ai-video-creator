package com.avc.infra.entity;

import com.avc.common.enums.Platform;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@TableName("scheduled_tasks")
@Getter
@Setter
@NoArgsConstructor
public class ScheduledTaskEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String jobId;

    private Platform platform;

    private Instant scheduledTime;

    private String status = "PENDING";

    private String error;

    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
}
