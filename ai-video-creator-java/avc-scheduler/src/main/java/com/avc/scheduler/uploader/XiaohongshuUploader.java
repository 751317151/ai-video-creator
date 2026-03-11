package com.avc.scheduler.uploader;

import com.avc.common.enums.Platform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Map;

@Slf4j
@Component
public class XiaohongshuUploader implements PlatformUploader {

    @Override
    public Platform getPlatform() {
        return Platform.XIAOHONGSHU;
    }

    @Override
    public String upload(Path videoPath, String title, String description,
                         String tags, Map<String, String> credentials) {
        log.info("Uploading to Xiaohongshu: title={}", title);
        throw new UnsupportedOperationException(
                "Xiaohongshu upload not yet implemented.");
    }

    @Override
    public boolean validateCredentials(Map<String, String> credentials) {
        return credentials != null && credentials.containsKey("accessToken");
    }
}
