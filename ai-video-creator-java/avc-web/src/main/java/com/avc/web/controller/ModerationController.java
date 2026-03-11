package com.avc.web.controller;

import com.avc.ai.moderation.ContentModerationService;
import com.avc.common.dto.request.ModerationCheckRequest;
import com.avc.common.dto.response.ApiResponse;
import com.avc.common.dto.response.ModerationResult;
import com.avc.infra.entity.ModerationLogEntity;
import com.avc.infra.mapper.ModerationLogMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
public class ModerationController {

    private final ContentModerationService moderationService;
    private final ModerationLogMapper moderationLogMapper;

    @PostMapping("/check")
    public ApiResponse<ModerationResult> check(@Valid @RequestBody ModerationCheckRequest request) {
        ModerationResult result = moderationService.moderate(request);
        return ApiResponse.ok(result);
    }

    @GetMapping("/log")
    public ApiResponse<List<ModerationLogEntity>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        IPage<ModerationLogEntity> logs = moderationLogMapper
                .selectPageOrdered(new Page<>(page + 1, size));
        return ApiResponse.ok(logs.getRecords());
    }
}
