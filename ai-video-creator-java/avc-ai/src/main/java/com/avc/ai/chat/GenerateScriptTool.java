package com.avc.ai.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenerateScriptTool {

    private final ChatClient chatClient;

    @Tool(description = "Generate a video script for a given topic. Use when user wants a script/outline for their video.")
    public String generateScript(
            @ToolParam(description = "Topic for the script") String topic,
            @ToolParam(description = "Video type: knowledge, news, story, tutorial") String videoType,
            @ToolParam(description = "Duration in seconds, default 60") int durationSeconds) {

        String prompt = String.format("""
                Generate a video script for the following:
                Topic: %s
                Type: %s
                Target duration: %d seconds

                Include: opening hook, main content segments, call to action.
                Format each segment with [timestamp] markers.
                """, topic, videoType, durationSeconds > 0 ? durationSeconds : 60);

        String script = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        log.info("Generated script for topic: {}", topic);
        return script;
    }
}
