package com.avc.ai.chat;

import com.avc.infra.cache.JobStatusCache;
import com.avc.infra.entity.JobEntity;
import com.avc.infra.mapper.JobMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CheckJobStatusTool {

    private final JobMapper jobMapper;
    private final JobStatusCache jobStatusCache;

    @Tool(description = "Check the status and progress of a video creation job. Use when user asks about job status/progress.")
    public String checkStatus(@ToolParam(description = "The job ID to check") String jobId) {
        return Optional.ofNullable(jobMapper.selectById(jobId))
                .map(job -> {
                    var cached = jobStatusCache.getProgress(jobId);
                    int percent = cached != null ? cached.percent() : job.getProgress();
                    String msg = cached != null ? cached.message() : job.getProgressMessage();

                    return String.format("Job %s: status=%s, progress=%d%%, message=%s",
                            jobId, job.getStatus(), percent, msg != null ? msg : "N/A");
                })
                .orElse("Job not found: " + jobId);
    }
}
