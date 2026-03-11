package com.avc.web.controller;

import com.avc.ai.rag.ScriptKnowledgeService;
import com.avc.common.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final ScriptKnowledgeService scriptKnowledgeService;

    @PostMapping("/ingest")
    public ApiResponse<String> ingest(@RequestBody Map<String, String> request) {
        String title = request.getOrDefault("title", "");
        String content = request.getOrDefault("content", "");
        String videoType = request.getOrDefault("video_type", "");
        String tags = request.getOrDefault("tags", "");
        scriptKnowledgeService.ingest(title, content, videoType, tags);
        return ApiResponse.ok("Script ingested successfully");
    }

    @GetMapping("/search")
    public ApiResponse<List<String>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK) {
        return ApiResponse.ok(scriptKnowledgeService.search(query, topK));
    }
}
