package com.avc.ai.script;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Enhanced script generation service with multiple video type templates.
 * Can be called independently (not just via Chat Tool).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScriptGenerationService {

    private final ChatClient chatClient;

    private static final Map<String, String> TYPE_PROMPTS = Map.of(
            "knowledge", """
                    Create an educational knowledge video script about: %s
                    Structure: Hook (5s) -> Problem (10s) -> Key Points (40-60s) -> Summary (10s)
                    Tone: Informative, clear, engaging
                    """,
            "news", """
                    Create a news report video script about: %s
                    Structure: Breaking headline (5s) -> Context (10s) -> Details (20-30s) -> Impact (10s)
                    Tone: Objective, authoritative, concise
                    """,
            "story", """
                    Create a storytelling video script about: %s
                    Structure: Opening hook (5s) -> Setup (15s) -> Rising tension (20-30s) -> Climax (10s) -> Resolution (10s)
                    Tone: Emotional, dramatic, suspenseful
                    """,
            "product", """
                    Create a product promotion video script about: %s
                    Structure: Pain point (5s) -> Solution intro (10s) -> Features (20s) -> Social proof (10s) -> CTA (5s)
                    Tone: Persuasive, energetic, trust-building
                    """
    );

    public String generateScript(String topic, String videoType, String extraRequirements) {
        String typeTemplate = TYPE_PROMPTS.getOrDefault(
                videoType != null ? videoType.toLowerCase() : "knowledge",
                TYPE_PROMPTS.get("knowledge"));

        String basePrompt = typeTemplate.formatted(topic);

        String fullPrompt = basePrompt + """

                Additional requirements: %s

                Return the script as JSON with this structure:
                {
                  "title": "Video title",
                  "scenes": [
                    {
                      "sceneNumber": 1,
                      "narration": "What to say",
                      "visualHint": "What to show",
                      "durationSeconds": 10
                    }
                  ],
                  "totalDurationSeconds": 60,
                  "tags": ["tag1", "tag2"]
                }
                """.formatted(extraRequirements != null ? extraRequirements : "None");

        return chatClient.prompt().user(fullPrompt).call().content();
    }
}
