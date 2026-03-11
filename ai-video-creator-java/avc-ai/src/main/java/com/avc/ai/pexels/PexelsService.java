package com.avc.ai.pexels;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Service
public class PexelsService {

    @Value("${avc.pexels-api-key:}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    public Optional<String> searchPhoto(String query) {
        if (!isAvailable() || query == null || query.isBlank()) {
            return Optional.empty();
        }

        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.pexels.com/v1/search?query=" + encoded + "&per_page=1&orientation=landscape"))
                    .header("Authorization", apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Pexels API returned status {}", response.statusCode());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode photos = root.get("photos");
            if (photos != null && photos.isArray() && !photos.isEmpty()) {
                JsonNode src = photos.get(0).get("src");
                if (src != null && src.has("medium")) {
                    return Optional.of(src.get("medium").asText());
                }
            }
        } catch (Exception e) {
            log.warn("Pexels search failed for query '{}': {}", query, e.getMessage());
        }

        return Optional.empty();
    }
}
