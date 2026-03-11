package com.avc.scheduler.uploader;

import com.avc.common.enums.Platform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Map;

/**
 * YouTube uploader via YouTube Data API v3.
 */
@Slf4j
@Component
public class YouTubeUploader implements PlatformUploader {

    @Override
    public Platform getPlatform() {
        return Platform.YOUTUBE;
    }

    @Override
    public String upload(Path videoPath, String title, String description,
                         String tags, Map<String, String> credentials) {
        String accessToken = credentials.get("accessToken");

        log.info("Uploading to YouTube: title={}", title);

        // TODO: Implement actual YouTube Data API v3 integration
        // Step 1: POST https://www.googleapis.com/upload/youtube/v3/videos
        // with resumable upload
        // Requires OAuth2 access token

        throw new UnsupportedOperationException(
                "YouTube upload not yet implemented. Configure OAuth credentials first.");
    }

    @Override
    public boolean validateCredentials(Map<String, String> credentials) {
        return credentials != null && credentials.containsKey("accessToken");
    }
}
