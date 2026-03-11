package com.avc.infra.mapper;

import com.avc.common.enums.ModerationVerdict;
import com.avc.infra.entity.ModerationLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ModerationLogMapper extends BaseMapper<ModerationLogEntity> {

    IPage<ModerationLogEntity> selectPageOrdered(Page<ModerationLogEntity> page);

    IPage<ModerationLogEntity> selectByVerdictPaged(Page<ModerationLogEntity> page, @Param("verdict") String verdict);
}
