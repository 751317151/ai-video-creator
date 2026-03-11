package com.avc.infra.entity;

import com.avc.common.enums.ModerationVerdict;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@TableName("moderation_log")
@Getter
@Setter
@NoArgsConstructor
public class ModerationLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private ModerationVerdict verdict;

    private double confidence;

    private String flaggedIssues;

    private String suggestion;

    private String checkedContent;

    private String contentType;

    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;
}
