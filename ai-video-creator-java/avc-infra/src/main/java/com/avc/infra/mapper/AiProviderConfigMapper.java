package com.avc.infra.mapper;

import com.avc.infra.entity.AiProviderConfigEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiProviderConfigMapper extends BaseMapper<AiProviderConfigEntity> {

    AiProviderConfigEntity findByProviderName(@Param("providerName") String providerName);

    List<AiProviderConfigEntity> findByEnabledTrue();
}
