package com.avc.web.controller;

import com.avc.ai.quality.VideoQualityService;
import com.avc.common.dto.request.QualityEvaluateRequest;
import com.avc.common.dto.response.ApiResponse;
import com.avc.common.dto.response.QualityReport;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quality")
@RequiredArgsConstructor
public class QualityController {

    private final VideoQualityService videoQualityService;

    @PostMapping("/evaluate")
    public ApiResponse<QualityReport> evaluate(@Valid @RequestBody QualityEvaluateRequest request) {
        QualityReport report = videoQualityService.evaluate(request);
        return ApiResponse.ok(report);
    }

    @GetMapping("/report/{jobId}")
    public ApiResponse<QualityReport> getReport(@PathVariable String jobId) {
        QualityReport report = videoQualityService.getReport(jobId);
        return ApiResponse.ok(report);
    }
}
