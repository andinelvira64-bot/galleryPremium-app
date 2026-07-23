package com.elvira.gallery;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Other apps (file managers, chat apps, etc.) hand us a content:// or
 * file:// Uri when the user picks "Open with our gallery". Every existing
 * feature here (crop, delete, capture-frame, effects) is built around plain
 * absolute file paths, so this resolves that Uri back to one wherever
 * possible, instead of teaching every feature to also understand Uris.
 */
public class ExternalUriResolver {

    private ExternalUriResolver() {}

    /** Returns an absolute file path for {@code uri}, or null if it truly
     *  can't be resolved to one (very rare for local files). */
    public static String resolveAbsolutePath(Context context, Uri uri) {
        if (uri == null) return null;

        if ("file".equals(uri.getScheme())) {
            return uri.getPath();
        }

        if ("content".equals(uri.getScheme())) {
            String fromDocumentId = tryDocumentIdPath(context, uri);
            if (fromDocumentId != null && new File(fromDocumentId).exists()) {
                return fromDocumentId;
            }

            String fromDataColumn = tryDataColumn(context, uri);
            if (fromDataColumn != null && new File(fromDataColumn).exists()) {
                return fromDataColumn;
            }

            // Last resort: copy the bytes into our own cache and use that.
            // Any edit/delete from here on will only affect this copy, not
            // whatever the other app originally pointed at, but viewing,
            // effects, and frame-capture still work fine either way.
            return copyToCache(context, uri);
        }

        return null;
    }

    /** Handles the common "primary:Some/Relative/Path.jpg" document-id shape
     *  used by Android's built-in local-storage document provider and many
     *  file managers that mimic it. */
    private static String tryDocumentIdPath(Context context, Uri uri) {
        try {
            String docId = DocumentsContract.isDocumentUri(context, uri)
                    ? DocumentsContract.getDocumentId(uri)
                    : null;
            if (docId == null) return null;

            int colon = docId.indexOf(':');
            if (colon < 0) return null;

            String type = docId.substring(0, colon);
            String relativePath = docId.substring(colon + 1);

            if ("primary".equalsIgnoreCase(type)) {
                return new File(Environment.getExternalStorageDirectory(), relativePath).getAbsolutePath();
            }
            // Non-"primary" usually means an SD card, identified by its
            // volume id rather than a path we can build directly - not
            // handled here, falls through to the other strategies.
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String tryDataColumn(Context context, Uri uri) {
        String[] projection = {MediaStore.MediaColumns.DATA};
        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                if (idx >= 0) return cursor.getString(idx);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String copyToCache(Context context, Uri uri) {
        try {
            String displayName = queryDisplayName(context, uri);
            if (displayName == null) displayName = "opened_" + System.currentTimeMillis();

            File outFile = new File(context.getCacheDir(), displayName);
            try (InputStream in = context.getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(outFile)) {
                if (in == null) return null;
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            return outFile.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    private static String queryDisplayName(Context context, Uri uri) {
        String[] projection = {android.provider.OpenableColumns.DISPLAY_NAME};
        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return cursor.getString(idx);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
