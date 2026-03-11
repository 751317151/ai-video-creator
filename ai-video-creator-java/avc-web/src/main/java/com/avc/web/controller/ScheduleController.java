package com.avc.web.controller;

import com.avc.common.dto.response.ApiResponse;
import com.avc.common.enums.Platform;
import com.avc.infra.entity.PublishRecordEntity;
import com.avc.infra.entity.ScheduledTaskEntity;
import com.avc.scheduler.job.ScheduleService;
import com.avc.scheduler.uploader.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final UploadService uploadService;

    @GetMapping
    public ApiResponse<List<ScheduledTaskEntity>> listTasks() {
        return ApiResponse.ok(scheduleService.listScheduledTasks());
    }

    @GetMapping("/{jobId}")
    public ApiResponse<List<ScheduledTaskEntity>> getTasksForJob(@PathVariable String jobId) {
        return ApiResponse.ok(scheduleService.getTasksForJob(jobId));
    }

    @PostMapping
    public ApiResponse<ScheduledTaskEntity> schedulePublish(
            @RequestParam String jobId,
            @RequestParam Platform platform,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant scheduledTime) {
        return ApiResponse.ok(scheduleService.schedulePublish(jobId, platform, scheduledTime));
    }

    @DeleteMapping("/{taskId}")
    public ApiResponse<Void> cancelTask(@PathVariable Long taskId) {
        scheduleService.cancelScheduledTask(taskId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/platforms")
    public ApiResponse<List<Platform>> availablePlatforms() {
        return ApiResponse.ok(uploadService.getAvailablePlatforms());
    }

    @GetMapping("/publish-history/{jobId}")
    public ApiResponse<List<PublishRecordEntity>> publishHistory(@PathVariable String jobId) {
        return ApiResponse.ok(uploadService.getPublishHistory(jobId));
    }
}
