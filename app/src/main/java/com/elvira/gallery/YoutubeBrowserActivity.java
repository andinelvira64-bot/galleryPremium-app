package com.elvira.gallery;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.List;

/**
 * A plain WebView pointed at youtube.com - this is the real YouTube website,
 * with real ads and normal playback, just opened inside this app instead of
 * a separate browser. It cannot navigate anywhere outside YouTube's own
 * domains: any link to a different site is blocked rather than followed.
 *
 * This does NOT download, scrape, or otherwise bypass anything - it's the
 * same experience as visiting youtube.com in a browser tab.
 */
public class YoutubeBrowserActivity extends AppCompatActivity {

    private static final String START_URL = "https://www.youtube.com/";

    // Domains YouTube's own site legitimately needs to function (playback,
    // Google sign-in, thumbnails, static assets). Anything outside this list
    // gets blocked rather than loaded.
    private static final List<String> ALLOWED_DOMAIN_SUFFIXES = Arrays.asList(
            "youtube.com",
            "youtube-nocookie.com",
            "ytimg.com",
            "googlevideo.com",
            "googleapis.com",
            "gstatic.com",
            "google.com",
            "accounts.google.com",
            "googleusercontent.com",
            "doubleclick.net"
    );

    private WebView webView;
    private ProgressBar progressBar;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youtube_browser);

        webView = findViewById(R.id.youtubeWebView);
        progressBar = findViewById(R.id.youtubeProgress);
        ImageButton btnBack = findViewById(R.id.btnYoutubeBack);
        ImageButton btnReload = findViewById(R.id.btnYoutubeReload);

        btnBack.setOnClickListener(v -> {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                finish();
            }
        });
        btnReload.setOnClickListener(v -> webView.reload());

        // YouTube's own site requires JavaScript and DOM storage to work at
        // all (playback, search, sign-in) - this only affects pages within
        // the allowed YouTube/Google domains above, nothing else.
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (isAllowedDomain(uri)) {
                    return false; // let the WebView load it normally
                }
                Toast.makeText(YoutubeBrowserActivity.this, R.string.youtube_blocked_domain, Toast.LENGTH_SHORT).show();
                return true; // block navigation to anything outside YouTube/Google
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }
        });

        webView.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }
        });

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(START_URL);
            Toast.makeText(this, R.string.youtube_login_note, Toast.LENGTH_LONG).show();
        }
    }

    private boolean isAllowedDomain(Uri uri) {
        String host = uri.getHost();
        if (host == null) return false;
        host = host.toLowerCase(java.util.Locale.ROOT);
        for (String suffix : ALLOWED_DOMAIN_SUFFIXES) {
            if (host.equals(suffix) || host.endsWith("." + suffix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }
}
