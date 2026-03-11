package com.avc.infra.mapper;

import com.avc.infra.entity.PublishRecordEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PublishRecordMapper extends BaseMapper<PublishRecordEntity> {

    List<PublishRecordEntity> findByJobIdOrderByCreatedAtDesc(@Param("jobId") String jobId);

    List<PublishRecordEntity> findByStatusOrderByCreatedAtDesc(@Param("status") String status);
}
