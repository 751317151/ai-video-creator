package com.avc.scheduler.uploader;

import com.avc.common.enums.Platform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Map;

@Slf4j
@Component
public class KuaishouUploader implements PlatformUploader {

    @Override
    public Platform getPlatform() {
        return Platform.KUAISHOU;
    }

    @Override
    public String upload(Path videoPath, String title, String description,
                         String tags, Map<String, String> credentials) {
        log.info("Uploading to Kuaishou: title={}", title);
        throw new UnsupportedOperationException(
                "Kuaishou upload not yet implemented.");
    }

    @Override
    public boolean validateCredentials(Map<String, String> credentials) {
        return credentials != null && credentials.containsKey("accessToken");
    }
}
