package com.avc.web.controller;

import com.avc.ai.chat.VideoChatService;
import com.avc.common.dto.request.ChatRequest;
import com.avc.common.dto.response.ApiResponse;
import com.avc.common.dto.response.ChatResponse;
import com.avc.infra.cache.ChatSessionStore;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final VideoChatService videoChatService;
    private final ChatSessionStore chatSessionStore;

    @PostMapping
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = videoChatService.chat(request);
        return ApiResponse.ok(response);
    }

    @GetMapping("/sessions/{sessionId}/history")
    public ApiResponse<List<Map<String, String>>> getHistory(@PathVariable String sessionId) {
        return ApiResponse.ok(chatSessionStore.getHistory(sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<String> clearSession(@PathVariable String sessionId) {
        chatSessionStore.clearSession(sessionId);
        return ApiResponse.ok("Session cleared");
    }
}
