package com.avc.infra.entity;

import com.avc.common.enums.Platform;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@TableName("platform_credentials")
@Getter
@Setter
@NoArgsConstructor
public class PlatformCredentialEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Platform platform;

    private String credentialData;

    private boolean enabled = true;

    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
}
