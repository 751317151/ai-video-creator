package com.avc.app.service;

import com.avc.ai.moderation.ContentModerationService;
import com.avc.common.dto.request.ModerationCheckRequest;
import com.avc.common.dto.response.ModerationResult;
import com.avc.infra.entity.ModerationLogEntity;
import com.avc.infra.mapper.ModerationLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentModerationServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private ModerationLogMapper moderationLogMapper;

    private ContentModerationService service;

    @BeforeEach
    void setUp() {
        service = new ContentModerationService(chatClient, moderationLogMapper);
    }

    @Test
    void shouldReturnSafeWhenContentIsSafe() {
        String aiResponse = """
                {"safe": true, "riskLevel": "LOW", "issues": [], "suggestion": "Content is appropriate"}
                """;

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(aiResponse);

        ModerationCheckRequest request = new ModerationCheckRequest("This is a nice video about cooking", "script");
        ModerationResult result = service.moderate(request);

        assertThat(result.safe()).isTrue();
        assertThat(result.riskLevel()).isEqualTo("LOW");
        assertThat(result.issues()).isEmpty();
        verify(moderationLogMapper).insert(any(ModerationLogEntity.class));
    }

    @Test
    void shouldReturnFlaggedWhenContentIsUnsafe() {
        String aiResponse = """
                {"safe": false, "riskLevel": "HIGH", "issues": ["violence"], "suggestion": "Contains violent content"}
                """;

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(aiResponse);

        ModerationCheckRequest request = new ModerationCheckRequest("violent content here", "script");
        ModerationResult result = service.moderate(request);

        assertThat(result.safe()).isFalse();
        assertThat(result.riskLevel()).isEqualTo("HIGH");
        assertThat(result.issues()).contains("violence");
        verify(moderationLogMapper).insert(any(ModerationLogEntity.class));
    }

    @Test
    void shouldFailClosedWhenAiServiceFails() {
        when(chatClient.prompt()).thenThrow(new RuntimeException("AI service down"));

        ModerationCheckRequest request = new ModerationCheckRequest("test content", "script");
        ModerationResult result = service.moderate(request);

        assertThat(result.safe()).isFalse();
        assertThat(result.riskLevel()).isEqualTo("HIGH");
        assertThat(result.issues()).contains("Moderation service error");
    }

    @Test
    void shouldFailClosedWhenResponseUnparseable() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("not valid json at all {{{");

        ModerationCheckRequest request = new ModerationCheckRequest("test content", "script");
        ModerationResult result = service.moderate(request);

        assertThat(result.safe()).isFalse();
        assertThat(result.riskLevel()).isEqualTo("HIGH");
        assertThat(result.issues()).contains("Failed to parse moderation response");
    }

    @Test
    void shouldHandleJsonWrappedInCodeBlock() {
        String aiResponse = """
                ```json
                {"safe": true, "riskLevel": "LOW", "issues": [], "suggestion": "All clear"}
                ```
                """;

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(aiResponse);

        ModerationCheckRequest request = new ModerationCheckRequest("nice content", "script");
        ModerationResult result = service.moderate(request);

        assertThat(result.safe()).isTrue();
        assertThat(result.riskLevel()).isEqualTo("LOW");
    }
}
