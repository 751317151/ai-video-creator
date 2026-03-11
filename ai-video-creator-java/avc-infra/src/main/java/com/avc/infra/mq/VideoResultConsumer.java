package com.avc.infra.mq;

import com.avc.common.dto.mq.VideoResultMessage;
import com.avc.common.enums.JobStatus;
import com.avc.infra.entity.JobEntity;
import com.avc.infra.mapper.JobMapper;
import com.avc.infra.service.StatisticsService;
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
        topic = "video-result",
        consumerGroup = "avc-result-consumer-group",
        maxReconsumeTimes = 3
)
public class VideoResultConsumer implements RocketMQListener<String> {

    private final JobMapper jobMapper;
    private final ProgressBroadcaster progressBroadcaster;
    private final StatisticsService statisticsService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(String raw) {
        try {
            VideoResultMessage msg = objectMapper.readValue(raw, VideoResultMessage.class);
            log.info("Received video result: jobId={}, status={}", msg.jobId(), msg.status());

            Optional.ofNullable(jobMapper.selectById(msg.jobId())).ifPresent(job -> {
                if ("COMPLETED".equals(msg.status())) {
                    job.setStatus(JobStatus.COMPLETED);
                    job.setProgress(100);
                    job.setVideoPath(msg.videoPath());
                    job.setThumbnailPath(msg.thumbnailPath());
                    job.setTitle(msg.title());
                    job.setDescription(msg.description());
                    job.setDurationSeconds(msg.durationSeconds());

                    // Record statistics
                    try {
                        statisticsService.recordStat(
                                msg.jobId(),
                                msg.title(),
                                job.getVideoType() != null ? job.getVideoType().name() : null,
                                msg.durationSeconds(),
                                msg.fileSizeBytes(),
                                msg.generationTimeMs()
                        );
                    } catch (Exception e) {
                        log.warn("Failed to record statistics for job {}", msg.jobId(), e);
                    }
                } else {
                    job.setStatus(JobStatus.FAILED);
                    job.setError(msg.error());
                }
                jobMapper.updateById(job);
                progressBroadcaster.broadcast(msg.jobId(), job.getProgress(),
                        job.getStatus().name(), job.getError());
            });
        } catch (Exception e) {
            log.error("Failed to process video result", e);
        }
    }
}
