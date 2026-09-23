package com.napjak.scanner;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

/**
 * 앱 캐시 폴더의 파일(카메라 촬영본, 공유용 PDF)을 다른 앱에 잠깐 빌려주는 최소 Provider.
 * content://com.napjak.scanner.files/{capture|share}/{파일이름}
 */
public class CacheProvider extends ContentProvider {
    static final String AUTHORITY = "com.napjak.scanner.files";

    static Uri uriFor(String dir, String name) {
        return new Uri.Builder().scheme("content").authority(AUTHORITY)
                .appendPath(dir).appendPath(name).build();
    }

    static File fileFor(Context ctx, Uri uri) {
        if (ctx == null || uri == null || !AUTHORITY.equals(uri.getAuthority())) return null;
        List<String> seg = uri.getPathSegments();
        if (seg.size() != 2) return null;
        String dir = seg.get(0), name = seg.get(1);
        if (!"capture".equals(dir) && !"share".equals(dir)) return null;
        if (name.isEmpty() || name.contains("/") || name.startsWith(".")) return null;
        return new File(new File(ctx.getCacheDir(), dir), name);
    }

    @Override public boolean onCreate() { return true; }

    @Override public String getType(Uri uri) {
        String p = uri.getLastPathSegment();
        if (p != null && p.toLowerCase().endsWith(".pdf")) return "application/pdf";
        return "image/jpeg";
    }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File f = fileFor(getContext(), uri);
        if (f == null) throw new FileNotFoundException(String.valueOf(uri));
        File parent = f.getParentFile();
        if (parent != null) parent.mkdirs();
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.parseMode(mode));
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        File f = fileFor(getContext(), uri);
        if (f == null) return null;
        String[] cols = projection != null ? projection
                : new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        MatrixCursor c = new MatrixCursor(cols, 1);
        Object[] row = new Object[cols.length];
        for (int i = 0; i < cols.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(cols[i])) row[i] = f.getName();
            else if (OpenableColumns.SIZE.equals(cols[i])) row[i] = f.length();
        }
        c.addRow(row);
        return c;
    }

    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String selection, String[] args) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { return 0; }
}
