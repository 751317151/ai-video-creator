package com.avc.ai.moderation;

import com.avc.common.dto.request.ModerationCheckRequest;
import com.avc.common.dto.response.ModerationResult;
import com.avc.common.enums.ModerationVerdict;
import com.avc.infra.entity.ModerationLogEntity;
import com.avc.infra.mapper.ModerationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentModerationService {

    private final ChatClient chatClient;
    private final ModerationLogMapper moderationLogMapper;

    public ModerationResult moderate(ModerationCheckRequest request) {
        String prompt = String.format("""
                Analyze the following content for safety and appropriateness.
                Content type: %s
                Content: %s

                Evaluate for: hate speech, violence, sexual content, misinformation, spam, copyright issues.

                Respond in JSON format:
                {
                  "safe": true/false,
                  "riskLevel": "LOW/MEDIUM/HIGH",
                  "issues": ["issue1", "issue2"],
                  "suggestion": "your suggestion"
                }
                """, request.contentType(), request.content());

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            ModerationResult result = parseResult(response);
            saveModerationLog(request, result);
            return result;
        } catch (Exception e) {
            log.error("Moderation failed", e);
            return new ModerationResult(false, "HIGH", List.of("Moderation service error"), "Manual review required");
        }
    }

    private ModerationResult parseResult(String response) {
        try {
            String cleaned = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = om.readTree(cleaned);
            boolean safe = node.path("safe").asBoolean(true);
            String riskLevel = node.path("riskLevel").asText("LOW");
            List<String> issues = new java.util.ArrayList<>();
            node.path("issues").forEach(n -> issues.add(n.asText()));
            String suggestion = node.path("suggestion").asText("");
            return new ModerationResult(safe, riskLevel, issues, suggestion);
        } catch (Exception e) {
            log.warn("Failed to parse moderation response, treating as flagged (fail-closed)", e);
            return new ModerationResult(false, "HIGH",
                    List.of("Failed to parse moderation response"),
                    "Manual review required - moderation response unparseable");
        }
    }

    private void saveModerationLog(ModerationCheckRequest request, ModerationResult result) {
        ModerationLogEntity logEntity = new ModerationLogEntity();
        logEntity.setVerdict(result.safe() ? ModerationVerdict.SAFE : ModerationVerdict.FLAGGED);
        logEntity.setConfidence(switch (result.riskLevel()) {
            case "HIGH" -> 0.9;
            case "MEDIUM" -> 0.7;
            default -> 0.5;
        });
        logEntity.setFlaggedIssues(String.join(", ", result.issues()));
        logEntity.setSuggestion(result.suggestion());
        logEntity.setCheckedContent(request.content());
        logEntity.setContentType(request.contentType());
        moderationLogMapper.insert(logEntity);
    }
}
