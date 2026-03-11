package com.avc.scheduler.job;

import com.avc.common.enums.Platform;
import com.avc.infra.entity.ScheduledTaskEntity;
import com.avc.infra.mapper.ScheduledTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Quartz Job that executes a scheduled publish task.
 */
@Slf4j
@Component
public class PublishExecutorJob implements Job {

    @Autowired
    private ScheduledTaskMapper scheduledTaskMapper;

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        long taskId = dataMap.getLong("taskId");
        String jobId = dataMap.getString("jobId");
        String platformStr = dataMap.getString("platform");

        log.info("Executing scheduled publish: taskId={}, jobId={}, platform={}", taskId, jobId, platformStr);

        Optional.ofNullable(scheduledTaskMapper.selectById(taskId)).ifPresent(task -> {
            try {
                task.setStatus("EXECUTING");
                scheduledTaskMapper.updateById(task);

                // TODO: Call UploadService.uploadToPlatform() here
                // This requires fetching credentials from PlatformCredentialRepository
                // and the video path from JobEntity

                task.setStatus("COMPLETED");
                scheduledTaskMapper.updateById(task);
                log.info("Scheduled publish completed: taskId={}", taskId);
            } catch (Exception e) {
                task.setStatus("FAILED");
                task.setError(e.getMessage());
                scheduledTaskMapper.updateById(task);
                log.error("Scheduled publish failed: taskId={}", taskId, e);
            }
        });
    }
}
