package com.avc.infra.mq;

import com.avc.common.dto.mq.VideoTaskMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoTaskProducer {

    private static final String TOPIC = "video-task-submit";

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    public void sendCreateTask(VideoTaskMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            rocketMQTemplate.convertAndSend(TOPIC + ":CREATE", json);
            log.info("Sent video task: jobId={}, topic={}", message.jobId(), message.topic());
        } catch (Exception e) {
            log.error("Failed to send video task for job {}", message.jobId(), e);
            throw new RuntimeException("Failed to send MQ message", e);
        }
    }

    public void sendCancelTask(String jobId) {
        try {
            String json = objectMapper.writeValueAsString(VideoTaskMessage.cancel(jobId));
            rocketMQTemplate.convertAndSend(TOPIC + ":CANCEL", json);
            log.info("Sent cancel task: jobId={}", jobId);
        } catch (Exception e) {
            log.error("Failed to send cancel for job {}", jobId, e);
        }
    }
}
