package com.avc.common.enums;

/**
 * Logical category for stored media files.
 */
public enum StorageType {

    VIDEO("videos"),
    AUDIO("audio"),
    IMAGE("images"),
    SUBTITLE("subtitles"),
    CLIP("clips"),
    THUMBNAIL("thumbnails"),
    MUSIC("music");

    private final String folder;

    StorageType(String folder) {
        this.folder = folder;
    }

    public String getFolder() {
        return folder;
    }
}
