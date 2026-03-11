package com.avc.app.service;

import com.avc.app.BaseIntegrationTest;
import com.avc.common.dto.request.PresetCreateRequest;
import com.avc.common.enums.PresetCategory;
import com.avc.common.exception.BusinessException;
import com.avc.infra.entity.PresetEntity;
import com.avc.infra.mapper.PresetMapper;
import com.avc.infra.service.PresetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PresetServiceTest extends BaseIntegrationTest {

    @Autowired
    private PresetService presetService;

    @Autowired
    private PresetMapper presetMapper;

    @BeforeEach
    void setUp() {
        presetMapper.delete(null);
    }

    @Test
    void shouldCreatePresetWithDefaults() {
        PresetCreateRequest request = new PresetCreateRequest(
                "Test Preset", "A test description", PresetCategory.KNOWLEDGE,
                "knowledge", "zh-CN-XiaoxiaoNeural", "+0%",
                null, null, null, null, null, null
        );

        PresetEntity created = presetService.createPreset(request);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Test Preset");
        assertThat(created.getCategory()).isEqualTo(PresetCategory.KNOWLEDGE);
        assertThat(created.isBuiltin()).isFalse();
        assertThat(created.getMinDuration()).isEqualTo(60);
        assertThat(created.getMaxDuration()).isEqualTo(90);
        assertThat(created.getSubtitleFontSize()).isEqualTo(52);
        assertThat(created.getSubtitleColor()).isEqualTo("white");
    }

    @Test
    void shouldCreatePresetWithCustomValues() {
        PresetCreateRequest request = new PresetCreateRequest(
                "Custom Preset", "Custom desc", PresetCategory.STORY,
                "story", "zh-CN-YunxiNeural", "-10%",
                "Extra requirements here", 30, 120, 60, "yellow", "tag1,tag2"
        );

        PresetEntity created = presetService.createPreset(request);

        assertThat(created.getMinDuration()).isEqualTo(30);
        assertThat(created.getMaxDuration()).isEqualTo(120);
        assertThat(created.getSubtitleFontSize()).isEqualTo(60);
        assertThat(created.getSubtitleColor()).isEqualTo("yellow");
        assertThat(created.getDefaultTags()).isEqualTo("tag1,tag2");
        assertThat(created.getExtraRequirements()).isEqualTo("Extra requirements here");
    }

    @Test
    void shouldGetPresetById() {
        PresetEntity saved = savePreset("Findable", PresetCategory.NEWS, false);

        PresetEntity found = presetService.getPreset(saved.getId());

        assertThat(found.getName()).isEqualTo("Findable");
    }

    @Test
    void shouldThrowWhenPresetNotFound() {
        assertThatThrownBy(() -> presetService.getPreset(99999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Preset not found");
    }

    @Test
    void shouldListAllPresetsOrderedByBuiltinDescNameAsc() {
        savePreset("Bravo", PresetCategory.KNOWLEDGE, false);
        savePreset("Alpha", PresetCategory.STORY, true);
        savePreset("Charlie", PresetCategory.NEWS, false);

        List<PresetEntity> presets = presetService.listPresets(null);

        assertThat(presets).hasSize(3);
        assertThat(presets.get(0).getName()).isEqualTo("Alpha");  // builtin first
        assertThat(presets.get(0).isBuiltin()).isTrue();
    }

    @Test
    void shouldListPresetsByCategory() {
        savePreset("Knowledge-1", PresetCategory.KNOWLEDGE, false);
        savePreset("Knowledge-2", PresetCategory.KNOWLEDGE, false);
        savePreset("Story-1", PresetCategory.STORY, false);

        List<PresetEntity> knowledgePresets = presetService.listPresets(PresetCategory.KNOWLEDGE);

        assertThat(knowledgePresets).hasSize(2);
        assertThat(knowledgePresets).allMatch(p -> p.getCategory() == PresetCategory.KNOWLEDGE);
    }

    @Test
    void shouldDeleteNonBuiltinPreset() {
        PresetEntity preset = savePreset("Deletable", PresetCategory.EDUCATION, false);

        presetService.deletePreset(preset.getId());

        assertThat(presetMapper.selectById(preset.getId())).isNull();
    }

    @Test
    void shouldRejectDeletingBuiltinPreset() {
        PresetEntity builtin = savePreset("Builtin Preset", PresetCategory.KNOWLEDGE, true);

        assertThatThrownBy(() -> presetService.deletePreset(builtin.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot delete builtin preset");
    }

    private PresetEntity savePreset(String name, PresetCategory category, boolean builtin) {
        PresetEntity entity = new PresetEntity();
        entity.setName(name);
        entity.setCategory(category);
        entity.setVideoType("knowledge");
        entity.setBuiltin(builtin);
        entity.setMinDuration(60);
        entity.setMaxDuration(90);
        entity.setSubtitleFontSize(48);
        entity.setSubtitleColor("white");
        presetMapper.insert(entity);
        return entity;
    }
}
