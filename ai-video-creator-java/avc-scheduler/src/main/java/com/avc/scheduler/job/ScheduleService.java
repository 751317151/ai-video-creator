package com.avc.scheduler.job;

import com.avc.common.enums.Platform;
import com.avc.common.exception.BusinessException;
import com.avc.infra.entity.ScheduledTaskEntity;
import com.avc.infra.mapper.ScheduledTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduledTaskMapper scheduledTaskMapper;
    private final Scheduler quartzScheduler;

    @Transactional
    public ScheduledTaskEntity schedulePublish(String jobId, Platform platform, Instant scheduledTime) {
        ScheduledTaskEntity task = new ScheduledTaskEntity();
        task.setJobId(jobId);
        task.setPlatform(platform);
        task.setScheduledTime(scheduledTime);
        task.setStatus("PENDING");
        scheduledTaskMapper.insert(task);

        try {
            JobDetail jobDetail = JobBuilder.newJob(PublishExecutorJob.class)
                    .withIdentity("publish-" + task.getId(), "publish-group")
                    .usingJobData("taskId", task.getId())
                    .usingJobData("jobId", jobId)
                    .usingJobData("platform", platform.name())
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-" + task.getId(), "publish-group")
                    .startAt(Date.from(scheduledTime))
                    .build();

            quartzScheduler.scheduleJob(jobDetail, trigger);
            log.info("Scheduled publish: taskId={}, jobId={}, platform={}, time={}",
                    task.getId(), jobId, platform, scheduledTime);
        } catch (SchedulerException e) {
            task.setStatus("FAILED");
            task.setError("Failed to schedule: " + e.getMessage());
            scheduledTaskMapper.updateById(task);
            throw new BusinessException("Failed to schedule publish: " + e.getMessage());
        }

        return task;
    }

    @Transactional
    public void cancelScheduledTask(Long taskId) {
        ScheduledTaskEntity task = Optional.ofNullable(scheduledTaskMapper.selectById(taskId))
                .orElseThrow(() -> new BusinessException("Scheduled task not found: " + taskId));

        if (!"PENDING".equals(task.getStatus())) {
            throw new BusinessException("Can only cancel pending tasks, current status: " + task.getStatus());
        }

        try {
            quartzScheduler.deleteJob(new JobKey("publish-" + taskId, "publish-group"));
        } catch (SchedulerException e) {
            log.warn("Failed to delete Quartz job for task {}", taskId, e);
        }

        task.setStatus("CANCELLED");
        scheduledTaskMapper.updateById(task);
    }

    public List<ScheduledTaskEntity> listScheduledTasks() {
        return scheduledTaskMapper.findAllByOrderByScheduledTimeAsc();
    }

    public List<ScheduledTaskEntity> getTasksForJob(String jobId) {
        return scheduledTaskMapper.findByJobIdOrderByScheduledTimeAsc(jobId);
    }
}
