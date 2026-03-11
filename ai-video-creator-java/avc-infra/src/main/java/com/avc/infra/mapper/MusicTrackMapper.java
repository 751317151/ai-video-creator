package com.avc.infra.mapper;

import com.avc.infra.entity.MusicTrackEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MusicTrackMapper extends BaseMapper<MusicTrackEntity> {

    List<MusicTrackEntity> findByCategoryOrderByNameAsc(@Param("category") String category);

    List<MusicTrackEntity> findByMoodOrderByUseCountDesc(@Param("mood") String mood);

    List<MusicTrackEntity> findAllByOrderByUseCountDesc();

    List<MusicTrackEntity> findTop10ByOrderByUseCountDesc();
}
