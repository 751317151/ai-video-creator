package com.avc.infra.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@TableName("ai_provider_config")
@Getter
@Setter
@NoArgsConstructor
public class AiProviderConfigEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String providerName;

    private String apiKeyEncrypted;

    private String baseUrl;

    private String modelName;

    private String description;

    private boolean enabled = true;

    private String extraConfig;

    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
}
