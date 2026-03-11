package com.avc.web.controller;

import com.avc.ai.optimizer.AiOptimizerService;
import com.avc.common.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiOptimizerController {

    private final AiOptimizerService aiOptimizerService;

    @PostMapping("/optimize-title")
    public ApiResponse<String> optimizeTitle(
            @RequestParam String title,
            @RequestParam(required = false) String platform) {
        return ApiResponse.ok(aiOptimizerService.optimizeTitle(title, platform));
    }

    @PostMapping("/suggest-tags")
    public ApiResponse<String> suggestTags(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "10") int maxTags) {
        return ApiResponse.ok(aiOptimizerService.suggestTags(title, description, maxTags));
    }

    @PostMapping("/seo-score")
    public ApiResponse<Map<String, Object>> seoScore(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String tags) {
        return ApiResponse.ok(aiOptimizerService.seoScore(title, description, tags));
    }

    @GetMapping("/trending")
    public ApiResponse<List<String>> trending(
            @RequestParam String niche,
            @RequestParam(defaultValue = "5") int count) {
        return ApiResponse.ok(aiOptimizerService.getTrendingTopics(niche, count));
    }
}
