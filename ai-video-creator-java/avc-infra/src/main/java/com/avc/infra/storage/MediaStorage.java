package com.avc.infra.storage;

import com.avc.common.enums.StorageType;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * Abstract interface for media file storage.
 * Implementations: LocalMediaStorage (default), S3MediaStorage.
 */
public interface MediaStorage {

    /**
     * Upload a local file to storage.
     *
     * @param localPath   path to the local file
     * @param storageType logical category (videos, audio, etc.)
     * @param filename    optional override for the stored filename; defaults to the local filename
     * @return the storage key (relative path like "videos/abc.mp4")
     */
    String upload(Path localPath, StorageType storageType, String filename);

    /**
     * Upload from an InputStream (useful for HTTP uploads).
     */
    String upload(InputStream inputStream, StorageType storageType, String filename, long contentLength);

    /**
     * Download a file from storage to a local path.
     */
    Path download(String key, Path localPath);

    /**
     * Get a URL for accessing the file. For local storage this is a relative API path;
     * for S3 this is a presigned URL.
     */
    String getUrl(String key, int expiresSeconds);

    /**
     * Delete a file from storage.
     */
    void delete(String key);

    /**
     * List files in a storage category.
     */
    List<String> listFiles(StorageType storageType, String prefix);

    /**
     * Check if a file exists.
     */
    boolean exists(String key);
}
