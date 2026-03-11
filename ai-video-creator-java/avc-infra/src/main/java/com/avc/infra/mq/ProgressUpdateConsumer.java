package com.avc.infra.mq;

import com.avc.common.dto.mq.ProgressUpdateMessage;
import com.avc.infra.cache.JobStatusCache;
import com.avc.infra.entity.JobEntity;
import com.avc.infra.mapper.JobMapper;
import com.avc.infra.websocket.ProgressBroadcaster;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "video-progress",
        consumerGroup = "avc-progress-consumer-group",
        maxReconsumeTimes = 3
)
public class ProgressUpdateConsumer implements RocketMQListener<String> {

    private final JobMapper jobMapper;
    private final JobStatusCache jobStatusCache;
    private final ProgressBroadcaster progressBroadcaster;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(String raw) {
        try {
            ProgressUpdateMessage msg = objectMapper.readValue(raw, ProgressUpdateMessage.class);
            log.debug("Progress update: jobId={}, percent={}", msg.jobId(), msg.percent());

            jobStatusCache.updateProgress(msg.jobId(), msg.percent(), msg.message());

            Optional.ofNullable(jobMapper.selectById(msg.jobId())).ifPresent(job -> {
                job.setProgress(msg.percent());
                job.setProgressMessage(msg.message());
                jobMapper.updateById(job);
            });

            progressBroadcaster.broadcast(msg.jobId(), msg.percent(), "RUNNING", msg.message());
        } catch (Exception e) {
            log.error("Failed to process progress update", e);
        }
    }
}
