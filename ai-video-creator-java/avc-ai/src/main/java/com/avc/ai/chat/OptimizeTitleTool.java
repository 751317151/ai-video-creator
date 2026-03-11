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
public class OptimizeTitleTool {

    private final ChatClient chatClient;

    @Tool(description = "Optimize a video title for SEO and engagement. Use when user wants to improve their video title.")
    public String optimizeTitle(
            @ToolParam(description = "Original video title") String title,
            @ToolParam(description = "Target platform: douyin, bilibili, youtube") String platform) {

        String prompt = String.format("""
                Optimize this video title for %s platform:
                Original: %s

                Provide 3 optimized alternatives with:
                1. SEO-friendly keywords
                2. Emotional hooks
                3. Appropriate length for the platform
                """, platform != null ? platform : "general", title);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
