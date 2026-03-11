package com.avc.infra.mapper;

import com.avc.infra.entity.PresetEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PresetMapper extends BaseMapper<PresetEntity> {

    List<PresetEntity> findByCategoryOrderByNameAsc(@Param("category") String category);

    List<PresetEntity> findAllOrdered();

    List<PresetEntity> findByBuiltinTrue();
}
