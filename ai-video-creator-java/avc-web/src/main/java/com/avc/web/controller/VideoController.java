package com.avc.web.controller;

import com.avc.common.dto.mq.VideoTaskMessage;
import com.avc.common.dto.request.VideoCreateRequest;
import com.avc.common.dto.response.ApiResponse;
import com.avc.common.dto.response.JobResponse;
import com.avc.common.enums.JobStatus;
import com.avc.common.util.IdGenerator;
import com.avc.infra.entity.JobEntity;
import com.avc.infra.mq.VideoTaskProducer;
import com.avc.infra.mapper.JobMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final JobMapper jobMapper;
    private final VideoTaskProducer videoTaskProducer;

    @PostMapping("/create")
    public ApiResponse<JobResponse> createVideo(@Valid @RequestBody VideoCreateRequest request) {
        String jobId = IdGenerator.jobId();

        JobEntity job = new JobEntity();
        job.setId(jobId);
        job.setTopic(request.topic());
        job.setVideoType(request.videoType() != null ?
                com.avc.common.enums.VideoType.valueOf(request.videoType().toUpperCase()) : null);
        job.setVideoSource(request.videoSource() != null ?
                com.avc.common.enums.VideoSource.valueOf(request.videoSource().toUpperCase()) : null);
        job.setVoice(request.voice());
        job.setExtraRequirements(request.extraRequirements());
        job.setTemplateId(request.templateId());
        job.setBgmTrackId(request.bgmTrackId());
        job.setBgmVolume(request.bgmVolume());
        job.setScriptJson(request.scriptJson());
        jobMapper.insert(job);

        VideoTaskMessage msg = new VideoTaskMessage(
                jobId, "CREATE", request.topic(), request.scriptJson(),
                request.videoType(), request.videoSource(),
                request.voice(), null,
                1080, 1920, 30, 48, "white",
                null, request.aiVideoProvider(), null,
                null, request.bgmVolume(),
                request.extraRequirements(), request.templateId());
        videoTaskProducer.sendCreateTask(msg);

        return ApiResponse.ok(toJobResponse(job));
    }

    @GetMapping("/{jobId}")
    public ApiResponse<JobResponse> getJob(@PathVariable String jobId) {
        return Optional.ofNullable(jobMapper.selectById(jobId))
                .map(job -> ApiResponse.ok(toJobResponse(job)))
                .orElse(ApiResponse.fail("Job not found: " + jobId));
    }

    @GetMapping
    public ApiResponse<List<JobResponse>> listJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        IPage<JobEntity> jobs = jobMapper.selectPageOrdered(new Page<>(page + 1, size));
        List<JobResponse> list = jobs.getRecords().stream().map(this::toJobResponse).toList();
        return ApiResponse.ok(list);
    }

    @PostMapping("/{jobId}/cancel")
    public ApiResponse<String> cancelJob(@PathVariable String jobId) {
        return Optional.ofNullable(jobMapper.selectById(jobId)).map(job -> {
            if (job.getStatus() == JobStatus.QUEUED || job.getStatus() == JobStatus.RUNNING) {
                job.setStatus(JobStatus.CANCELLED);
                jobMapper.updateById(job);
                videoTaskProducer.sendCancelTask(jobId);
                return ApiResponse.ok("Job cancelled");
            }
            return ApiResponse.<String>fail("Job cannot be cancelled in status: " + job.getStatus());
        }).orElse(ApiResponse.fail("Job not found: " + jobId));
    }

    private JobResponse toJobResponse(JobEntity job) {
        return new JobResponse(
                job.getId(), job.getStatus().name(), job.getTopic(),
                job.getVideoType() != null ? job.getVideoType().name() : null,
                job.getProgress(), job.getProgressMessage(),
                job.getVideoPath(), job.getThumbnailPath(),
                job.getTitle(), job.getDescription(),
                job.getDurationSeconds(), job.getError(),
                job.getCreatedAt(), job.getUpdatedAt());
    }
}
