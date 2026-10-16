package org.ranobe.ranobe.network;

import android.webkit.CookieManager;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * Shares cookies between OkHttp and every WebView in the app. A Cloudflare clearance earned in
 * a WebView is sent with the app's own requests, and the other way round.
 */
public class WebViewCookieJar implements CookieJar {
    private final CookieManager manager;

    private WebViewCookieJar(CookieManager manager) {
        this.manager = manager;
    }

    /**
     * Null when there is no WebView to share with (unit tests, or no WebView installed).
     */
    public static CookieJar createOrNull() {
        try {
            CookieManager manager = CookieManager.getInstance();
            manager.setAcceptCookie(true);
            return new WebViewCookieJar(manager);
        } catch (Throwable e) {
            // RuntimeException when the WebView package is missing, or a stub outside Android
            return null;
        }
    }

    /**
     * The value of one cookie for this url, or null.
     */
    public static String get(String url, String name) {
        try {
            String header = CookieManager.getInstance().getCookie(url);
            if (header == null) return null;
            for (String pair : header.split(";")) {
                String trimmed = pair.trim();
                if (trimmed.startsWith(name + "=")) return trimmed.substring(name.length() + 1);
            }
        } catch (Throwable ignored) {
            // no WebView
        }
        return null;
    }

    @Override
    public void saveFromResponse(@NonNull HttpUrl url, @NonNull List<Cookie> cookies) {
        String target = url.toString();
        for (Cookie cookie : cookies) manager.setCookie(target, cookie.toString());
    }

    @NonNull
    @Override
    public List<Cookie> loadForRequest(@NonNull HttpUrl url) {
        String header = manager.getCookie(url.toString());
        if (header == null || header.isEmpty()) return Collections.emptyList();
        List<Cookie> cookies = new ArrayList<>();
        for (String pair : header.split(";")) {
            Cookie cookie = Cookie.parse(url, pair.trim());
            if (cookie != null) cookies.add(cookie);
        }
        return cookies;
    }
}
