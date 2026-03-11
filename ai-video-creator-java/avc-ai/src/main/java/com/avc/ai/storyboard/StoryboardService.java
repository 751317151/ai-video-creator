package com.avc.ai.storyboard;

import com.avc.ai.pexels.PexelsService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates visual storyboards by using AI to create scene descriptions,
 * then fetches preview images from Pexels.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoryboardService {

    private final ChatClient chatClient;
    private final PexelsService pexelsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
            String cleaned = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            List<Map<String, Object>> scenes = objectMapper.readValue(cleaned, new TypeReference<>() {});

            if (pexelsService.isAvailable()) {
                for (int i = 0; i < scenes.size(); i++) {
                    Map<String, Object> scene = scenes.get(i);
                    String query = (String) scene.get("searchQuery");
                    Map<String, Object> enriched = new HashMap<>(scene);
                    pexelsService.searchPhoto(query)
                            .ifPresent(url -> enriched.put("imageUrl", url));
                    scenes.set(i, enriched);
                }
            }

            return scenes;
        } catch (Exception e) {
            log.warn("Failed to parse storyboard response", e);
            return List.of(Map.of(
                    "error", "Failed to generate storyboard",
                    "rawResponse", response
            ));
        }
    }
}
