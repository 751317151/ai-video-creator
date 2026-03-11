package com.avc.web.controller;

import com.avc.common.dto.response.ApiResponse;
import com.avc.infra.entity.VideoStatisticEntity;
import com.avc.infra.service.StatisticsService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary() {
        return ApiResponse.ok(statisticsService.getSummary());
    }

    @GetMapping("/videos")
    public ApiResponse<IPage<VideoStatisticEntity>> videos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(statisticsService.listVideos(page, size));
    }

    @GetMapping("/video-types")
    public ApiResponse<List<Map<String, Object>>> videoTypes() {
        return ApiResponse.ok(statisticsService.getVideoTypeBreakdown());
    }

    @GetMapping("/platforms")
    public ApiResponse<List<Map<String, Object>>> platforms() {
        return ApiResponse.ok(statisticsService.getPlatformBreakdown());
    }

    @GetMapping("/trends")
    public ApiResponse<List<Map<String, Object>>> trends(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(statisticsService.getDailyTrends(days));
    }

    @GetMapping("/performance")
    public ApiResponse<Map<String, Object>> performance() {
        return ApiResponse.ok(statisticsService.getPerformance());
    }
}
