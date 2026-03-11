package com.avc.app.service;

import com.avc.app.BaseIntegrationTest;
import com.avc.common.exception.BusinessException;
import com.avc.infra.entity.AiProviderConfigEntity;
import com.avc.infra.mapper.AiProviderConfigMapper;
import com.avc.infra.service.AiProviderConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiProviderConfigServiceTest extends BaseIntegrationTest {

    @Autowired
    private AiProviderConfigService configService;

    @Autowired
    private AiProviderConfigMapper configMapper;

    @BeforeEach
    void setUp() {
        configMapper.delete(null);
    }

    @Test
    void shouldSaveNewConfig() {
        AiProviderConfigEntity saved = configService.saveConfig(
                "zhipu", "sk-test-api-key-12345", "https://api.zhipuai.cn",
                "cogvideox", "ZhipuAI CogVideoX", null
        );

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getProviderName()).isEqualTo("zhipu");
        assertThat(saved.getBaseUrl()).isEqualTo("https://api.zhipuai.cn");
        assertThat(saved.getModelName()).isEqualTo("cogvideox");
        // returned entity should have masked API key
        assertThat(saved.getApiKeyEncrypted()).isEqualTo("********");
    }

    @Test
    void shouldUpdateExistingConfig() {
        configService.saveConfig("zhipu", "old-key", "https://old-url", "old-model", "old desc", null);

        AiProviderConfigEntity updated = configService.saveConfig(
                "zhipu", "new-key", "https://new-url", "new-model", "new desc", null
        );

        assertThat(updated.getBaseUrl()).isEqualTo("https://new-url");
        assertThat(updated.getModelName()).isEqualTo("new-model");
        assertThat(configMapper.selectCount(null)).isEqualTo(1);
    }

    @Test
    void shouldDecryptApiKey() {
        configService.saveConfig("zhipu", "secret-key-value", null, null, null, null);

        String decrypted = configService.getDecryptedApiKey("zhipu");

        assertThat(decrypted).isEqualTo("secret-key-value");
    }

    @Test
    void shouldMaskApiKeyInListConfigs() {
        configService.saveConfig("zhipu", "key1", null, null, null, null);
        configService.saveConfig("siliconflow", "key2", null, null, null, null);

        List<AiProviderConfigEntity> configs = configService.listConfigs();

        assertThat(configs).hasSize(2);
        assertThat(configs).allMatch(c -> "********".equals(c.getApiKeyEncrypted()));
    }

    @Test
    void shouldMaskApiKeyInGetConfig() {
        configService.saveConfig("zhipu", "my-secret-key", null, null, null, null);

        AiProviderConfigEntity config = configService.getConfig("zhipu");

        assertThat(config.getProviderName()).isEqualTo("zhipu");
        assertThat(config.getApiKeyEncrypted()).isEqualTo("********");
    }

    @Test
    void shouldThrowWhenConfigNotFound() {
        assertThatThrownBy(() -> configService.getConfig("nonexistent"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Provider config not found");
    }

    @Test
    void shouldThrowWhenDecryptingNonexistentProvider() {
        assertThatThrownBy(() -> configService.getDecryptedApiKey("nonexistent"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Provider config not found");
    }

    @Test
    void shouldTestConnection() {
        configService.saveConfig("zhipu", "key", null, null, null, null);

        boolean result = configService.testConnection("zhipu");

        assertThat(result).isTrue();
    }

    @Test
    void shouldPreserveFieldsWhenPartialUpdate() {
        configService.saveConfig("zhipu", "original-key", "https://api.zhipuai.cn", "model-v1", "desc", null);

        // Update only baseUrl, pass null for other fields
        configService.saveConfig("zhipu", null, "https://new-api.zhipuai.cn", null, null, null);

        String decryptedKey = configService.getDecryptedApiKey("zhipu");
        assertThat(decryptedKey).isEqualTo("original-key");

        AiProviderConfigEntity config = configMapper.findByProviderName("zhipu");
        assertThat(config.getBaseUrl()).isEqualTo("https://new-api.zhipuai.cn");
        assertThat(config.getModelName()).isEqualTo("model-v1");
    }
}
