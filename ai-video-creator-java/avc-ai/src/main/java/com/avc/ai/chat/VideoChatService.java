package com.avc.ai.chat;

import com.avc.common.dto.request.ChatRequest;
import com.avc.common.dto.response.ChatResponse;
import com.avc.common.util.IdGenerator;
import com.avc.infra.cache.ChatSessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoChatService {

    private final ChatClient chatClient;
    private final ChatSessionStore chatSessionStore;
    private final CreateVideoTool createVideoTool;
    private final GenerateScriptTool generateScriptTool;
    private final CheckJobStatusTool checkJobStatusTool;
    private final OptimizeTitleTool optimizeTitleTool;

    public ChatResponse chat(ChatRequest request) {
        String sessionId = request.sessionId() != null ? request.sessionId() : IdGenerator.uuid();
        String userMessage = request.message();

        chatSessionStore.appendMessage(sessionId, "user", userMessage);

        try {
            List<Map<String, String>> history = chatSessionStore.getHistory(sessionId);

            var promptBuilder = chatClient.prompt();

            // Inject conversation history (all messages except the last one which is the current user message)
            for (int i = 0; i < history.size() - 1; i++) {
                Map<String, String> msg = history.get(i);
                String role = msg.get("role");
                String content = msg.get("content");
                if ("user".equals(role)) {
                    promptBuilder.user(content);
                } else if ("assistant".equals(role)) {
                    promptBuilder.system(content);
                }
            }

            String reply = promptBuilder
                    .user(userMessage)
                    .tools(createVideoTool, generateScriptTool, checkJobStatusTool, optimizeTitleTool)
                    .call()
                    .content();

            chatSessionStore.appendMessage(sessionId, "assistant", reply);
            return new ChatResponse(sessionId, reply, null);
        } catch (Exception e) {
            log.error("Chat error for session {}", sessionId, e);
            String errorMsg = "AI service error: " + e.getMessage();
            return new ChatResponse(sessionId, null, errorMsg);
        }
    }
}
