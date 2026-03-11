package com.avc.infra.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProgressBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(String jobId, int percent, String status, String message) {
        Map<String, Object> payload = Map.of(
                "job_id", jobId,
                "percent", percent,
                "status", status,
                "message", message != null ? message : ""
        );
        messagingTemplate.convertAndSend("/topic/progress/" + jobId, payload);
        log.debug("Broadcast progress: jobId={}, percent={}, status={}", jobId, percent, status);
    }
}
