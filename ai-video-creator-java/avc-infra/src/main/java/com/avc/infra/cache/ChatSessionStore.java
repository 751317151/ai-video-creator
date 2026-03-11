package com.avc.infra.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSessionStore {

    private static final String KEY_PREFIX = "chat:session:";
    private static final Duration TTL = Duration.ofHours(2);
    private static final int MAX_MESSAGES = 50;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void appendMessage(String sessionId, String role, String content) {
        String key = KEY_PREFIX + sessionId;
        try {
            String entry = objectMapper.writeValueAsString(Map.of("role", role, "content", content));
            redisTemplate.opsForList().rightPush(key, entry);
            redisTemplate.opsForList().trim(key, -MAX_MESSAGES, -1);
            redisTemplate.expire(key, TTL);
        } catch (Exception e) {
            log.error("Failed to append chat message for session {}", sessionId, e);
        }
    }

    public List<Map<String, String>> getHistory(String sessionId) {
        String key = KEY_PREFIX + sessionId;
        List<String> raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<Map<String, String>> result = new ArrayList<>(raw.size());
        for (String entry : raw) {
            try {
                result.add(objectMapper.readValue(entry, new TypeReference<>() {}));
            } catch (Exception e) {
                log.warn("Skipping invalid chat entry", e);
            }
        }
        return result;
    }

    public void clearSession(String sessionId) {
        redisTemplate.delete(KEY_PREFIX + sessionId);
    }
}
