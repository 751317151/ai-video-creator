package com.avc.infra.service;

import com.avc.common.exception.BusinessException;
import com.avc.infra.entity.AiProviderConfigEntity;
import com.avc.infra.mapper.AiProviderConfigMapper;
import com.avc.infra.security.AesEncryptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiProviderConfigService {

    private final AiProviderConfigMapper configMapper;
    private final AesEncryptor aesEncryptor;

    public List<AiProviderConfigEntity> listConfigs() {
        List<AiProviderConfigEntity> configs = configMapper.selectList(null);
        configs.forEach(this::maskApiKey);
        return configs;
    }

    public AiProviderConfigEntity getConfig(String providerName) {
        AiProviderConfigEntity config = Optional.ofNullable(configMapper.findByProviderName(providerName))
                .orElseThrow(() -> new BusinessException("Provider config not found: " + providerName));
        maskApiKey(config);
        return config;
    }

    public String getDecryptedApiKey(String providerName) {
        AiProviderConfigEntity config = Optional.ofNullable(configMapper.findByProviderName(providerName))
                .orElseThrow(() -> new BusinessException("Provider config not found: " + providerName));
        return aesEncryptor.decrypt(config.getApiKeyEncrypted());
    }

    @Transactional
    public AiProviderConfigEntity saveConfig(String providerName, String apiKey, String baseUrl,
                                              String modelName, String description, String extraConfig) {
        AiProviderConfigEntity existing = configMapper.findByProviderName(providerName);
        boolean isNew = (existing == null);

        AiProviderConfigEntity config;
        if (isNew) {
            config = new AiProviderConfigEntity();
            config.setProviderName(providerName);
        } else {
            config = existing;
        }

        if (apiKey != null && !apiKey.isBlank()) {
            config.setApiKeyEncrypted(aesEncryptor.encrypt(apiKey));
        }
        if (baseUrl != null) {
            config.setBaseUrl(baseUrl);
        }
        if (modelName != null) {
            config.setModelName(modelName);
        }
        if (description != null) {
            config.setDescription(description);
        }
        if (extraConfig != null) {
            config.setExtraConfig(extraConfig);
        }

        if (isNew) {
            configMapper.insert(config);
        } else {
            configMapper.updateById(config);
        }
        log.info("Saved AI provider config: {}", providerName);
        maskApiKey(config);
        return config;
    }

    @Transactional
    public boolean testConnection(String providerName) {
        Optional.ofNullable(configMapper.findByProviderName(providerName))
                .orElseThrow(() -> new BusinessException("Provider config not found: " + providerName));
        // Actual connection test would be done here with the AI client
        // For now, return true if config exists
        return true;
    }

    private void maskApiKey(AiProviderConfigEntity config) {
        if (config.getApiKeyEncrypted() != null && !config.getApiKeyEncrypted().isBlank()) {
            config.setApiKeyEncrypted("********");
        }
    }
}
