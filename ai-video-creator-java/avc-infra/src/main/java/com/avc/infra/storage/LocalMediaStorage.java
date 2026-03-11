package com.avc.infra.storage;

import com.avc.common.enums.StorageType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Local filesystem storage backend -- default, zero-config.
 * Files are stored under {baseDir}/{category}/{filename}.
 */
@Slf4j
public class LocalMediaStorage implements MediaStorage {

    private final Path baseDir;

    public LocalMediaStorage(Path baseDir) {
        this.baseDir = baseDir;
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new StorageException("Failed to create base storage directory: " + baseDir, e);
        }
    }

    @Override
    public String upload(Path localPath, StorageType storageType, String filename) {
        String fname = (filename != null && !filename.isBlank()) ? filename : localPath.getFileName().toString();
        String key = makeKey(storageType, fname);
        Path dest = resolvePath(key);

        if (dest.toAbsolutePath().equals(localPath.toAbsolutePath())) {
            return key;
        }

        try {
            Files.createDirectories(dest.getParent());
            Files.copy(localPath, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Failed to copy file to local storage: " + key, e);
        }

        log.debug("Local upload: {} -> {}", localPath, key);
        return key;
    }

    @Override
    public String upload(InputStream inputStream, StorageType storageType, String filename, long contentLength) {
        String key = makeKey(storageType, filename);
        Path dest = resolvePath(key);

        try {
            Files.createDirectories(dest.getParent());
            Files.copy(inputStream, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Failed to write stream to local storage: " + key, e);
        }

        log.debug("Local stream upload: {} ({} bytes)", key, contentLength);
        return key;
    }

    @Override
    public Path download(String key, Path localPath) {
        Path src = resolvePath(key);
        if (!Files.exists(src)) {
            throw new StorageException("File not found in local storage: " + key);
        }

        if (localPath.toAbsolutePath().equals(src.toAbsolutePath())) {
            return localPath;
        }

        try {
            Files.createDirectories(localPath.getParent());
            Files.copy(src, localPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Failed to download from local storage: " + key, e);
        }
        return localPath;
    }

    @Override
    public String getUrl(String key, int expiresSeconds) {
        return "/api/storage/" + key;
    }

    @Override
    public void delete(String key) {
        Path path = resolvePath(key);
        try {
            Files.deleteIfExists(path);
            log.debug("Local delete: {}", key);
        } catch (IOException e) {
            throw new StorageException("Failed to delete file from local storage: " + key, e);
        }
    }

    @Override
    public List<String> listFiles(StorageType storageType, String prefix) {
        Path categoryDir = baseDir.resolve(storageType.getFolder());
        if (!Files.isDirectory(categoryDir)) {
            return List.of();
        }

        String filterPrefix = (prefix != null) ? prefix : "";
        List<String> results = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(categoryDir,
                entry -> entry.getFileName().toString().startsWith(filterPrefix))) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    results.add(makeKey(storageType, path.getFileName().toString()));
                }
            }
        } catch (IOException e) {
            throw new StorageException("Failed to list files in local storage: " + storageType.getFolder(), e);
        }

        results.sort(Comparator.naturalOrder());
        return results;
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(resolvePath(key));
    }

    /**
     * Get the actual local filesystem path for a key.
     * Specific to LocalMediaStorage for serving files via FileResponse.
     */
    public Path getLocalPath(String key) {
        return resolvePath(key);
    }

    private String makeKey(StorageType storageType, String filename) {
        return storageType.getFolder() + "/" + filename;
    }

    private Path resolvePath(String key) {
        Path resolved = baseDir.resolve(key).normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new StorageException("Path traversal detected in key: " + key);
        }
        return resolved;
    }
}
