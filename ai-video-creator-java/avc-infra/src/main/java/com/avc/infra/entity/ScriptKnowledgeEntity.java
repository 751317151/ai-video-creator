package com.avc.infra.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@TableName("script_knowledge")
@Getter
@Setter
@NoArgsConstructor
public class ScriptKnowledgeEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String videoType;

    private String tags;

    private String scriptContent;

    private int viewCount;

    private int likeCount;

    private String jobId;

    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;
}
