package com.avc.scheduler.uploader;

import com.avc.common.enums.Platform;
import com.avc.common.exception.BusinessException;
import com.avc.infra.entity.PublishRecordEntity;
import com.avc.infra.mapper.PublishRecordMapper;
import com.avc.infra.security.AesEncryptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates multi-platform video uploads.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {

    private final List<PlatformUploader> uploaders;
    private final PublishRecordMapper publishRecordMapper;
    private final AesEncryptor aesEncryptor;

    public PlatformUploader getUploader(Platform platform) {
        return uploaders.stream()
                .filter(u -> u.getPlatform() == platform)
                .findFirst()
                .orElseThrow(() -> new BusinessException("No uploader found for platform: " + platform));
    }

    @Transactional
    public PublishRecordEntity uploadToPlatform(String jobId, Platform platform,
                                                 Path videoPath, String title,
                                                 String description, String tags,
                                                 Map<String, String> credentials) {
        PublishRecordEntity record = new PublishRecordEntity();
        record.setJobId(jobId);
        record.setPlatform(platform);
        record.setStatus("UPLOADING");
        publishRecordMapper.insert(record);

        try {
            PlatformUploader uploader = getUploader(platform);
            String platformVideoId = uploader.upload(videoPath, title, description, tags, credentials);

            record.setStatus("PUBLISHED");
            record.setPlatformVideoId(platformVideoId);
            log.info("Published to {}: jobId={}, platformVideoId={}", platform, jobId, platformVideoId);
        } catch (Exception e) {
            record.setStatus("FAILED");
            record.setError(e.getMessage());
            log.error("Failed to publish to {}: jobId={}", platform, jobId, e);
        }

        publishRecordMapper.updateById(record);
        return record;
    }

    public List<PublishRecordEntity> getPublishHistory(String jobId) {
        return publishRecordMapper.findByJobIdOrderByCreatedAtDesc(jobId);
    }

    public List<Platform> getAvailablePlatforms() {
        return uploaders.stream().map(PlatformUploader::getPlatform).toList();
    }
}
