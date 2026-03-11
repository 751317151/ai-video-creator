package com.avc.infra.mapper;

import com.avc.infra.entity.VideoStatisticEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Mapper
public interface VideoStatisticMapper extends BaseMapper<VideoStatisticEntity> {

    VideoStatisticEntity findByJobId(@Param("jobId") String jobId);

    IPage<VideoStatisticEntity> selectPageOrdered(Page<VideoStatisticEntity> page);

    long countSince(@Param("since") Instant since);

    long totalViews();

    long totalLikes();

    double avgDuration();

    double avgGenerationTimeMs();

    List<Map<String, Object>> countByVideoType();

    List<Map<String, Object>> countByPlatform();

    List<Map<String, Object>> dailyCountSince(@Param("since") Instant since);

    double avgFileSize();

    long totalShares();
}
