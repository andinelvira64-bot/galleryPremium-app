package com.elvira.gallery;

import java.io.File;

/**
 * Represents a single photo or video file found on disk.
 */
public class MediaItem {

    public static final int TYPE_PHOTO = 0;
    public static final int TYPE_VIDEO = 1;

    public final File file;
    public final int type;
    public final long dateModified;

    public MediaItem(File file, int type, long dateModified) {
        this.file = file;
        this.type = type;
        this.dateModified = dateModified;
    }

    public boolean isVideo() {
        return type == TYPE_VIDEO;
    }

    public String getPath() {
        return file.getAbsolutePath();
    }

    public String getName() {
        return file.getName();
    }
}
