package com.avc.ai.optimizer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * AI-powered content optimization: title, tags, SEO scoring, trending topics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiOptimizerService {

    private final ChatClient chatClient;

    public String optimizeTitle(String originalTitle, String platform) {
        String prompt = """
                Optimize the following video title for the platform "%s".
                Make it catchy, SEO-friendly, and under 80 characters.
                Original title: %s

                Return ONLY the optimized title, nothing else.
                """.formatted(platform != null ? platform : "general", originalTitle);

        return chatClient.prompt().user(prompt).call().content();
    }

    public String suggestTags(String title, String description, int maxTags) {
        String prompt = """
                Suggest %d relevant tags/hashtags for a video with:
                Title: %s
                Description: %s

                Return tags as a comma-separated list, no '#' prefix.
                """.formatted(maxTags > 0 ? maxTags : 10, title,
                description != null ? description : "");

        return chatClient.prompt().user(prompt).call().content();
    }

    public Map<String, Object> seoScore(String title, String description, String tags) {
        String prompt = """
                Evaluate the SEO quality of this video content and return a JSON object:
                Title: %s
                Description: %s
                Tags: %s

                Return JSON with: {"score": 0-100, "titleScore": 0-100, "descriptionScore": 0-100,
                "tagScore": 0-100, "suggestions": ["suggestion1", "suggestion2"]}
                """.formatted(title,
                description != null ? description : "",
                tags != null ? tags : "");

        String response = chatClient.prompt().user(prompt).call().content();
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            String cleaned = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            return om.readValue(cleaned, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse SEO score response", e);
            return Map.of("score", 0, "error", "Failed to parse AI response");
        }
    }

    public List<String> getTrendingTopics(String niche, int count) {
        String prompt = """
                Suggest %d trending video topics for the niche: %s

                Return as a JSON array of strings, e.g. ["topic1", "topic2"]
                """.formatted(count > 0 ? count : 5, niche);

        String response = chatClient.prompt().user(prompt).call().content();
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            String cleaned = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            return om.readValue(cleaned, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse trending topics response", e);
            return List.of();
        }
    }
}
