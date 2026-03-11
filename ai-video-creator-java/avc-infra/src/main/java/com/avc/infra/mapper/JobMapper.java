package com.avc.infra.mapper;

import com.avc.common.enums.JobStatus;
import com.avc.infra.entity.JobEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface JobMapper extends BaseMapper<JobEntity> {

    IPage<JobEntity> selectPageOrdered(Page<JobEntity> page);

    List<JobEntity> selectByStatus(@Param("status") String status);

    long countByStatus(@Param("status") String status);
}
