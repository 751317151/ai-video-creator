package com.avc.web.controller;

import com.avc.common.dto.request.PresetCreateRequest;
import com.avc.common.dto.response.ApiResponse;
import com.avc.common.enums.PresetCategory;
import com.avc.infra.entity.PresetEntity;
import com.avc.infra.service.PresetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/presets")
@RequiredArgsConstructor
public class PresetController {

    private final PresetService presetService;

    @GetMapping
    public ApiResponse<List<PresetEntity>> list(
            @RequestParam(required = false) PresetCategory category) {
        return ApiResponse.ok(presetService.listPresets(category));
    }

    @GetMapping("/{id}")
    public ApiResponse<PresetEntity> get(@PathVariable Long id) {
        return ApiResponse.ok(presetService.getPreset(id));
    }

    @PostMapping
    public ApiResponse<PresetEntity> create(@Valid @RequestBody PresetCreateRequest request) {
        return ApiResponse.ok(presetService.createPreset(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        presetService.deletePreset(id);
        return ApiResponse.ok(null);
    }
}
