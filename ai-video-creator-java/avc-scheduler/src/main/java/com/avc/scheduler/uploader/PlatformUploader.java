package com.avc.scheduler.uploader;

import com.avc.common.enums.Platform;

import java.nio.file.Path;
import java.util.Map;

/**
 * Interface for platform-specific video uploaders.
 */
public interface PlatformUploader {

    Platform getPlatform();

    /**
     * Upload a video to the platform.
     *
     * @param videoPath  local path to the video file
     * @param title      video title
     * @param description video description
     * @param tags       comma-separated tags
     * @param credentials platform credentials map
     * @return platform-specific video ID
     */
    String upload(Path videoPath, String title, String description,
                  String tags, Map<String, String> credentials);

    /**
     * Check if the credentials are valid.
     */
    boolean validateCredentials(Map<String, String> credentials);
}
