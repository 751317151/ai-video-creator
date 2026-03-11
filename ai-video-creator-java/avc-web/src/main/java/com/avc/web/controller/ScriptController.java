package com.avc.web.controller;

import com.avc.ai.script.ScriptGenerationService;
import com.avc.ai.storyboard.StoryboardService;
import com.avc.common.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scripts")
@RequiredArgsConstructor
public class ScriptController {

    private final ScriptGenerationService scriptGenerationService;
    private final StoryboardService storyboardService;

    @PostMapping("/generate")
    public ApiResponse<String> generateScript(
            @RequestParam String topic,
            @RequestParam(defaultValue = "knowledge") String videoType,
            @RequestParam(required = false) String extraRequirements) {
        String script = scriptGenerationService.generateScript(topic, videoType, extraRequirements);
        return ApiResponse.ok(script);
    }

    @PostMapping("/storyboard")
    public ApiResponse<List<Map<String, Object>>> generateStoryboard(
            @RequestParam String topic,
            @RequestParam(required = false) String scriptJson,
            @RequestParam(defaultValue = "5") int sceneCount) {
        return ApiResponse.ok(storyboardService.generate(topic, scriptJson, sceneCount));
    }
}
