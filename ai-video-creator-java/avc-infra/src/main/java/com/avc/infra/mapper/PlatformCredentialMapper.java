package com.avc.infra.mapper;

import com.avc.infra.entity.PlatformCredentialEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PlatformCredentialMapper extends BaseMapper<PlatformCredentialEntity> {

    PlatformCredentialEntity findByPlatformAndEnabledTrue(@Param("platform") String platform);
}
