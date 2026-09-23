package com.napjak.scanner;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private static final int REQ_FILE = 101;

    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private Uri captureUri;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        web.setBackgroundColor(getColor(R.color.bg));
        web.addJavascriptInterface(new Bridge(), "Android");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                if ("file".equals(u.getScheme())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) { }
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                captureUri = null;
                Intent intent;
                if (params.isCaptureEnabled()) {
                    File dir = new File(getCacheDir(), "capture");
                    dir.mkdirs();
                    File[] old = dir.listFiles();
                    if (old != null) for (File f : old) f.delete();
                    captureUri = CacheProvider.uriFor("capture", "photo_" + System.currentTimeMillis() + ".jpg");
                    intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, captureUri);
                    intent.setClipData(ClipData.newRawUri("photo", captureUri));
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } else {
                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("image/*");
                    if (params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE) {
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    }
                }
                try {
                    startActivityForResult(intent, REQ_FILE);
                } catch (Exception e) {
                    fileCallback.onReceiveValue(null);
                    fileCallback = null;
                    captureUri = null;
                    Toast.makeText(MainActivity.this, "카메라나 사진 앱을 열 수 없어요", Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });

        if (state != null) web.restoreState(state);
        else web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req != REQ_FILE || fileCallback == null) {
            super.onActivityResult(req, res, data);
            return;
        }
        Uri[] result = null;
        if (res == RESULT_OK) {
            if (captureUri != null) {
                File f = CacheProvider.fileFor(this, captureUri);
                if (f != null && f.length() > 0) result = new Uri[]{captureUri};
            } else if (data != null) {
                ClipData clip = data.getClipData();
                if (clip != null && clip.getItemCount() > 0) {
                    result = new Uri[clip.getItemCount()];
                    for (int i = 0; i < clip.getItemCount(); i++) result[i] = clip.getItemAt(i).getUri();
                } else if (data.getData() != null) {
                    result = new Uri[]{data.getData()};
                }
            }
        }
        fileCallback.onReceiveValue(result);
        fileCallback = null;
        captureUri = null;
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("(window.napjakBack && window.napjakBack()) ? '1' : '0'", v -> {
            if (!"\"1\"".equals(v)) finish();
        });
    }

    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); web.saveState(out); }
    @Override protected void onPause() { super.onPause(); web.onPause(); }
    @Override protected void onResume() { super.onResume(); web.onResume(); }

    private static String safeName(String name) {
        String n = name == null ? "" : name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]+", "_").trim();
        if (n.isEmpty() || n.startsWith(".")) n = "scan" + n;
        if (!n.toLowerCase().endsWith(".pdf")) n += ".pdf";
        return n;
    }

    /** 페이지 JS에서 window.Android.savePdf / sharePdf 로 부른다. */
    public class Bridge {
        @JavascriptInterface
        public boolean isApp() { return true; }

        @JavascriptInterface
        public String savePdf(String b64, String name) {
            try {
                byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
                String n = safeName(name);
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.MediaColumns.DISPLAY_NAME, n);
                    cv.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                    cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/납작스캐너");
                    Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                    if (u == null) return "error:저장 위치를 만들 수 없어요";
                    try (OutputStream os = getContentResolver().openOutputStream(u)) {
                        if (os == null) return "error:파일을 쓸 수 없어요";
                        os.write(bytes);
                    }
                    return "ok:다운로드/납작스캐너 폴더에 저장했어요";
                } else {
                    File dir = new File(getExternalFilesDir(null), "PDF");
                    dir.mkdirs();
                    File f = new File(dir, n);
                    try (FileOutputStream fo = new FileOutputStream(f)) { fo.write(bytes); }
                    return "ok:" + f.getAbsolutePath() + " 에 저장했어요";
                }
            } catch (Exception e) {
                return "error:" + e.getMessage();
            }
        }

        @JavascriptInterface
        public String sharePdf(String b64, String name) {
            try {
                byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
                File dir = new File(getCacheDir(), "share");
                dir.mkdirs();
                File[] old = dir.listFiles();
                if (old != null) for (File f : old) f.delete();
                File f = new File(dir, safeName(name));
                try (FileOutputStream fo = new FileOutputStream(f)) { fo.write(bytes); }
                Uri u = CacheProvider.uriFor("share", f.getName());
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("application/pdf");
                send.putExtra(Intent.EXTRA_STREAM, u);
                send.setClipData(ClipData.newRawUri(f.getName(), u));
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Intent chooser = Intent.createChooser(send, "PDF 보내기");
                runOnUiThread(() -> startActivity(chooser));
                return "ok";
            } catch (Exception e) {
                return "error:" + e.getMessage();
            }
        }
    }
}
