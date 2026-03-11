package com.avc.infra.entity;

import com.avc.common.enums.MusicCategory;
import com.avc.common.enums.MusicMood;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@TableName("music_tracks")
@Getter
@Setter
@NoArgsConstructor
public class MusicTrackEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String artist;

    private MusicCategory category;

    private MusicMood mood;

    private int durationSeconds;

    private String storageKey;

    private long fileSize;

    private int useCount;

    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;
}
