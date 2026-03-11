package com.avc.infra.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Slf4j
@Configuration
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "avc.storage.type", havingValue = "local", matchIfMissing = true)
    public MediaStorage localMediaStorage(
            @Value("${avc.storage.local-dir:./output}") String localDir) {
        log.info("Initializing local media storage at: {}", localDir);
        return new LocalMediaStorage(Path.of(localDir));
    }

    @Bean
    @ConditionalOnProperty(name = "avc.storage.type", havingValue = "s3")
    public MediaStorage s3MediaStorage(
            @Value("${avc.storage.s3.endpoint-url:}") String endpointUrl,
            @Value("${avc.storage.s3.access-key}") String accessKey,
            @Value("${avc.storage.s3.secret-key}") String secretKey,
            @Value("${avc.storage.s3.bucket}") String bucket,
            @Value("${avc.storage.s3.region:}") String region,
            @Value("${avc.storage.s3.prefix:ai-video-creator}") String prefix) {
        return new S3MediaStorage(endpointUrl, accessKey, secretKey, bucket, region, prefix);
    }
}
