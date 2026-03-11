package com.avc.infra.cache;

import com.avc.common.dto.mq.ProgressUpdateMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobStatusCache {

    private static final String KEY_PREFIX = "job:status:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void updateProgress(String jobId, int percent, String message) {
        String key = KEY_PREFIX + jobId;
        redisTemplate.opsForHash().putAll(key, Map.of(
                "percent", String.valueOf(percent),
                "message", message
        ));
        redisTemplate.expire(key, TTL);
    }

    public ProgressUpdateMessage getProgress(String jobId) {
        String key = KEY_PREFIX + jobId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) {
            return null;
        }
        int percent = Integer.parseInt((String) entries.getOrDefault("percent", "0"));
        String message = (String) entries.getOrDefault("message", "");
        return new ProgressUpdateMessage(jobId, percent, message);
    }

    public void remove(String jobId) {
        redisTemplate.delete(KEY_PREFIX + jobId);
    }
}
