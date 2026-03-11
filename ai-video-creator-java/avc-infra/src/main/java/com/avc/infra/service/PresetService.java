package com.avc.infra.service;

import com.avc.common.dto.request.PresetCreateRequest;
import com.avc.common.enums.PresetCategory;
import com.avc.common.exception.BusinessException;
import com.avc.infra.entity.PresetEntity;
import com.avc.infra.mapper.PresetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PresetService {

    private final PresetMapper presetMapper;

    public List<PresetEntity> listPresets(PresetCategory category) {
        if (category != null) {
            return presetMapper.findByCategoryOrderByNameAsc(category.name());
        }
        return presetMapper.findAllOrdered();
    }

    public PresetEntity getPreset(Long id) {
        return Optional.ofNullable(presetMapper.selectById(id))
                .orElseThrow(() -> new BusinessException("Preset not found: " + id));
    }

    @Transactional
    public PresetEntity createPreset(PresetCreateRequest request) {
        PresetEntity entity = new PresetEntity();
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setCategory(request.category());
        entity.setVideoType(request.videoType());
        entity.setVoice(request.voice());
        entity.setTtsRate(request.ttsRate());
        entity.setExtraRequirements(request.extraRequirements());
        entity.setMinDuration(request.minDuration() != null ? request.minDuration() : 60);
        entity.setMaxDuration(request.maxDuration() != null ? request.maxDuration() : 90);
        entity.setSubtitleFontSize(request.subtitleFontSize() != null ? request.subtitleFontSize() : 52);
        entity.setSubtitleColor(request.subtitleColor() != null ? request.subtitleColor() : "white");
        entity.setDefaultTags(request.defaultTags());
        entity.setBuiltin(false);
        presetMapper.insert(entity);
        return entity;
    }

    @Transactional
    public void deletePreset(Long id) {
        PresetEntity entity = getPreset(id);
        if (entity.isBuiltin()) {
            throw new BusinessException("Cannot delete builtin preset: " + entity.getName());
        }
        presetMapper.deleteById(entity.getId());
    }
}
