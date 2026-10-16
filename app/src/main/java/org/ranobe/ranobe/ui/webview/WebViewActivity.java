package org.ranobe.ranobe.ui.webview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.databinding.ActivityWebviewBinding;
import org.ranobe.ranobe.network.Cloudflare;
import org.ranobe.ranobe.network.HttpClient;
import org.ranobe.ranobe.network.WebViewCookieJar;

/**
 * A site opened in a real browser view, to pass a Cloudflare check by hand. It shares cookies and
 * the user agent with the app's own requests, so once the page loads the app can reach the site
 * too. Closes itself as soon as the clearance cookie shows up.
 */
public class WebViewActivity extends AppCompatActivity {
    private static final String EXTRA_URL = "url";

    private ActivityWebviewBinding binding;
    private String url;
    private String initialClearance;

    /**
     * Opens the url, the result tells whether the check was passed.
     */
    public static class Verify extends ActivityResultContract<String, Boolean> {
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, String url) {
            return new Intent(context, WebViewActivity.class).putExtra(EXTRA_URL, url);
        }

        @Override
        public Boolean parseResult(int resultCode, @Nullable Intent intent) {
            return resultCode == RESULT_OK;
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityWebviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        url = getIntent().getStringExtra(EXTRA_URL);
        if (url == null) {
            finish();
            return;
        }
        initialClearance = WebViewCookieJar.get(url, Cloudflare.CLEARANCE_COOKIE);
        binding.toolbar.setTitle(R.string.cloudflare_verify_title);
        binding.toolbar.setSubtitle(Uri.parse(url).getHost());
        binding.toolbar.setNavigationOnClickListener(v -> finishVerified());
        binding.done.setOnClickListener(v -> finishVerified());

        WebView web = binding.web;
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        String agent = Cloudflare.userAgentFor(url);
        settings.setUserAgentString(agent != null ? agent : HttpClient.userAgent());
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                binding.progress.setProgressCompat(progress, true);
                binding.progress.setVisibility(progress < 100 ? View.VISIBLE : View.INVISIBLE);
            }
        });
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String pageUrl, Bitmap favicon) {
                binding.progress.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String pageUrl) {
                // the check is passed once a fresh clearance is set and the real page shows
                String clearance = WebViewCookieJar.get(url, Cloudflare.CLEARANCE_COOKIE);
                String title = view.getTitle();
                boolean stillChecking = title != null && title.contains("Just a moment");
                if (clearance != null && !clearance.equals(initialClearance) && !stillChecking) {
                    finishVerified();
                }
            }
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.web.canGoBack()) binding.web.goBack();
                else finishVerified();
            }
        });
        if (savedInstanceState == null) web.loadUrl(url);
        else web.restoreState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        binding.web.saveState(outState);
    }

    // the caller retries either way, a clearance may have been set without the page telling us
    private void finishVerified() {
        CookieManager.getInstance().flush();
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (binding != null) {
            binding.web.stopLoading();
            binding.web.destroy();
        }
        super.onDestroy();
    }
}
