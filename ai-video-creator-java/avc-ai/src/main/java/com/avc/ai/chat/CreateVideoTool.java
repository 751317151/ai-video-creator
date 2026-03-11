package com.avc.ai.chat;

import com.avc.common.dto.mq.VideoTaskMessage;
import com.avc.common.enums.JobStatus;
import com.avc.common.util.IdGenerator;
import com.avc.infra.entity.JobEntity;
import com.avc.infra.mq.VideoTaskProducer;
import com.avc.infra.mapper.JobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateVideoTool {

    private final JobMapper jobMapper;
    private final VideoTaskProducer videoTaskProducer;

    @Tool(description = "Create a video generation task. Use when user wants to create/make/generate a video.")
    public String createVideo(
            @ToolParam(description = "Video topic or subject") String topic,
            @ToolParam(description = "Video type: knowledge, news, story, tutorial") String videoType,
            @ToolParam(description = "Video source: pexels_video, ai_generated") String videoSource) {

        String jobId = IdGenerator.jobId();
        JobEntity job = new JobEntity();
        job.setId(jobId);
        job.setTopic(topic);
        job.setStatus(JobStatus.QUEUED);
        jobMapper.insert(job);

        VideoTaskMessage msg = new VideoTaskMessage(
                jobId, "CREATE", topic, null, videoType, videoSource,
                null, null,
                1080, 1920, 30, 48, "white",
                null, null, null,
                null, 0.3,
                null, null);
        videoTaskProducer.sendCreateTask(msg);

        log.info("Created video task via chat: jobId={}, topic={}", jobId, topic);
        return String.format("Video creation task submitted! Job ID: %s. Topic: %s. I'll track the progress for you.", jobId, topic);
    }
}
