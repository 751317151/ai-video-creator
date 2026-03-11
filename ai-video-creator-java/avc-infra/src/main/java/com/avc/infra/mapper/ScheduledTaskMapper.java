package com.avc.infra.mapper;

import com.avc.infra.entity.ScheduledTaskEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

@Mapper
public interface ScheduledTaskMapper extends BaseMapper<ScheduledTaskEntity> {

    List<ScheduledTaskEntity> findByStatusAndScheduledTimeBefore(@Param("status") String status, @Param("time") Instant time);

    List<ScheduledTaskEntity> findByJobIdOrderByScheduledTimeAsc(@Param("jobId") String jobId);

    List<ScheduledTaskEntity> findAllByOrderByScheduledTimeAsc();
}
