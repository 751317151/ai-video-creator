package com.avc.scheduler.uploader;

import com.avc.common.enums.Platform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Map;

/**
 * Douyin (TikTok China) uploader via Open Platform OAuth API.
 */
@Slf4j
@Component
public class DouyinUploader implements PlatformUploader {

    @Override
    public Platform getPlatform() {
        return Platform.DOUYIN;
    }

    @Override
    public String upload(Path videoPath, String title, String description,
                         String tags, Map<String, String> credentials) {
        String accessToken = credentials.get("accessToken");
        String openId = credentials.get("openId");

        log.info("Uploading to Douyin: title={}, openId={}", title, openId);

        // TODO: Implement actual Douyin Open Platform API integration
        // Step 1: POST /video/upload/ to upload video file
        // Step 2: POST /video/create/ with video_id + title + tags
        // Requires OAuth2 access token

        throw new UnsupportedOperationException(
                "Douyin upload not yet implemented. Configure OAuth credentials first.");
    }

    @Override
    public boolean validateCredentials(Map<String, String> credentials) {
        return credentials != null
                && credentials.containsKey("accessToken")
                && credentials.containsKey("openId");
    }
}
