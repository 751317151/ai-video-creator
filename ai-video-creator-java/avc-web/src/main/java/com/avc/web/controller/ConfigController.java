package com.avc.web.controller;

import com.avc.common.dto.response.ApiResponse;
import com.avc.infra.entity.AiProviderConfigEntity;
import com.avc.infra.service.AiProviderConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final AiProviderConfigService aiProviderConfigService;

    @GetMapping("/ai-providers")
    public ApiResponse<List<AiProviderConfigEntity>> listProviders() {
        return ApiResponse.ok(aiProviderConfigService.listConfigs());
    }

    @GetMapping("/ai-providers/{providerName}")
    public ApiResponse<AiProviderConfigEntity> getProvider(@PathVariable String providerName) {
        return ApiResponse.ok(aiProviderConfigService.getConfig(providerName));
    }

    @PutMapping("/ai-providers/{providerName}")
    public ApiResponse<AiProviderConfigEntity> saveProvider(
            @PathVariable String providerName,
            @RequestBody Map<String, String> body) {
        AiProviderConfigEntity saved = aiProviderConfigService.saveConfig(
                providerName,
                body.get("apiKey"),
                body.get("baseUrl"),
                body.get("modelName"),
                body.get("description"),
                body.get("extraConfig"));
        return ApiResponse.ok(saved);
    }

    @PostMapping("/ai-providers/{providerName}/test")
    public ApiResponse<Boolean> testConnection(@PathVariable String providerName) {
        return ApiResponse.ok(aiProviderConfigService.testConnection(providerName));
    }

    /**
     * Compatibility endpoint for Dashboard HTML which calls /api/config/ai-video.
     * Returns the same data as /api/config/ai-providers.
     */
    @GetMapping("/ai-video")
    public ApiResponse<List<AiProviderConfigEntity>> listProvidersCompat() {
        return ApiResponse.ok(aiProviderConfigService.listConfigs());
    }

    @PutMapping("/ai-video")
    public ApiResponse<String> saveProviderCompat(@RequestBody Map<String, String> body) {
        String providerName = body.getOrDefault("provider", "default");
        aiProviderConfigService.saveConfig(
                providerName,
                body.get("apiKey"),
                body.get("baseUrl"),
                body.get("modelName"),
                body.get("description"),
                body.get("extraConfig"));
        return ApiResponse.ok("saved");
    }
}
