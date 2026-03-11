package com.avc.infra.mapper;

import com.avc.infra.entity.ScriptKnowledgeEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ScriptKnowledgeMapper extends BaseMapper<ScriptKnowledgeEntity> {

    ScriptKnowledgeEntity findByJobId(@Param("jobId") String jobId);

    List<ScriptKnowledgeEntity> findByVideoTypeOrderByViewCountDesc(@Param("videoType") String videoType);

    long countByVideoType(@Param("videoType") String videoType);
}
