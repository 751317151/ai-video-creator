package com.avc.ai.storyboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Generates visual storyboards by using AI to create scene descriptions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoryboardService {

    private final ChatClient chatClient;

    public List<Map<String, Object>> generate(String topic, String scriptJson, int sceneCount) {
        String prompt = """
                Generate a visual storyboard for a video about: %s
                Number of scenes: %d
                %s

                For each scene, return a JSON array where each element has:
                {
                  "sceneNumber": 1,
                  "description": "What happens in this scene",
                  "visualDescription": "Detailed visual description for image search",
                  "durationSeconds": 10,
                  "narration": "What the narrator says",
                  "searchQuery": "Pexels search query for background"
                }

                Return ONLY the JSON array.
                """.formatted(
                topic,
                sceneCount > 0 ? sceneCount : 5,
                scriptJson != null ? "Script reference: " + scriptJson : "");

        String response = chatClient.prompt().user(prompt).call().content();

        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            String cleaned = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            return om.readValue(cleaned, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse storyboard response", e);
            return List.of(Map.of(
                    "error", "Failed to generate storyboard",
                    "rawResponse", response
            ));
        }
    }
}
