package com.avc.infra.storage;

import com.avc.common.enums.StorageType;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * S3-compatible object storage backend.
 * Works with AWS S3, MinIO, Alibaba Cloud OSS, Tencent COS.
 */
@Slf4j
public class S3MediaStorage implements MediaStorage {

    private static final Map<String, String> CONTENT_TYPE_MAP = Map.of(
            "mp4", "video/mp4",
            "mp3", "audio/mpeg",
            "wav", "audio/wav",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "srt", "text/plain",
            "json", "application/json"
    );

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;
    private final String prefix;

    public S3MediaStorage(String endpointUrl, String accessKey, String secretKey,
                          String bucket, String region, String prefix) {
        this.bucket = bucket;
        this.prefix = (prefix != null) ? prefix.replaceAll("^/+|/+$", "") : "ai-video-creator";

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);
        Region awsRegion = (region != null && !region.isBlank()) ? Region.of(region) : Region.US_EAST_1;

        S3ClientBuilder clientBuilder = S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(awsRegion);

        S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                .credentialsProvider(credentialsProvider)
                .region(awsRegion);

        if (endpointUrl != null && !endpointUrl.isBlank()) {
            URI endpoint = URI.create(endpointUrl);
            clientBuilder.endpointOverride(endpoint).forcePathStyle(true);
            presignerBuilder.endpointOverride(endpoint);
        }

        this.s3Client = clientBuilder.build();
        this.s3Presigner = presignerBuilder.build();

        log.info("S3 storage initialized: bucket={}, prefix={}, endpoint={}",
                bucket, this.prefix, endpointUrl != null ? endpointUrl : "default");
    }

    @Override
    public String upload(Path localPath, StorageType storageType, String filename) {
        String fname = (filename != null && !filename.isBlank()) ? filename : localPath.getFileName().toString();
        String s3Key = makeS3Key(storageType, fname);
        String storageKey = stripPrefix(s3Key);

        PutObjectRequest.Builder reqBuilder = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key);

        String contentType = guessContentType(fname);
        if (!contentType.isBlank()) {
            reqBuilder.contentType(contentType);
        }

        try {
            s3Client.putObject(reqBuilder.build(), localPath);
        } catch (Exception e) {
            throw new StorageException("S3 upload failed for key: " + s3Key, e);
        }

        log.debug("S3 upload: {} -> s3://{}/{}", localPath, bucket, s3Key);
        return storageKey;
    }

    @Override
    public String upload(InputStream inputStream, StorageType storageType, String filename, long contentLength) {
        String s3Key = makeS3Key(storageType, filename);
        String storageKey = stripPrefix(s3Key);

        PutObjectRequest.Builder reqBuilder = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentLength(contentLength);

        String contentType = guessContentType(filename);
        if (!contentType.isBlank()) {
            reqBuilder.contentType(contentType);
        }

        try {
            s3Client.putObject(reqBuilder.build(), RequestBody.fromInputStream(inputStream, contentLength));
        } catch (Exception e) {
            throw new StorageException("S3 stream upload failed for key: " + s3Key, e);
        }

        log.debug("S3 stream upload: {} ({} bytes)", storageKey, contentLength);
        return storageKey;
    }

    @Override
    public Path download(String key, Path localPath) {
        String s3Key = fullKey(key);

        try {
            Files.createDirectories(localPath.getParent());
            try (var response = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build())) {
                Files.copy(response, localPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (NoSuchKeyException e) {
            throw new StorageException("File not found in S3: " + key, e);
        } catch (IOException | S3Exception e) {
            throw new StorageException("S3 download failed for key: " + s3Key, e);
        }

        return localPath;
    }

    @Override
    public String getUrl(String key, int expiresSeconds) {
        String s3Key = fullKey(key);

        try {
            var presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(expiresSeconds))
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(s3Key)
                            .build())
                    .build();
            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            throw new StorageException("Failed to generate presigned URL for: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        String s3Key = fullKey(key);

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build());
            log.debug("S3 delete: s3://{}/{}", bucket, s3Key);
        } catch (S3Exception e) {
            throw new StorageException("S3 delete failed for key: " + s3Key, e);
        }
    }

    @Override
    public List<String> listFiles(StorageType storageType, String prefix) {
        String s3Prefix = makeS3Key(storageType, prefix != null ? prefix : "");
        List<String> results = new ArrayList<>();

        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(s3Prefix)
                    .build();

            var response = s3Client.listObjectsV2Paginator(request);
            for (var page : response) {
                for (var obj : page.contents()) {
                    results.add(stripPrefix(obj.key()));
                }
            }
        } catch (S3Exception e) {
            throw new StorageException("S3 list failed for prefix: " + s3Prefix, e);
        }

        results.sort(Comparator.naturalOrder());
        return results;
    }

    @Override
    public boolean exists(String key) {
        String s3Key = fullKey(key);

        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            throw new StorageException("S3 exists check failed for key: " + s3Key, e);
        }
    }

    private String makeS3Key(StorageType storageType, String filename) {
        return prefix + "/" + storageType.getFolder() + "/" + filename;
    }

    private String stripPrefix(String s3Key) {
        String prefixSlash = prefix + "/";
        if (s3Key.startsWith(prefixSlash)) {
            return s3Key.substring(prefixSlash.length());
        }
        return s3Key;
    }

    private String fullKey(String key) {
        return prefix + "/" + key;
    }

    private static String guessContentType(String filename) {
        int dotIdx = filename.lastIndexOf('.');
        if (dotIdx < 0) {
            return "";
        }
        String ext = filename.substring(dotIdx + 1).toLowerCase();
        return CONTENT_TYPE_MAP.getOrDefault(ext, "");
    }
}
