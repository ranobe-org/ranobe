package org.ranobe.ranobe.network;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.ranobe.ranobe.App;
import org.ranobe.ranobe.R;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Gets past Cloudflare's "Just a moment..." page the way a browser does: the page is loaded in an
 * invisible WebView, Cloudflare's own script runs there and sets the cf_clearance cookie, which
 * {@link WebViewCookieJar} then hands to every later request. The WebView uses the same user agent
 * as the app's requests, since the clearance is only accepted with the agent that earned it.
 * <p>
 * Challenges that want a human (a captcha) can't be solved invisibly, those requests fail with
 * {@link ChallengeException} and the screen offers to open the page in {@code WebViewActivity}.
 */
public final class Cloudflare {
    public static final String CLEARANCE_COOKIE = "cf_clearance";
    private static final long SOLVE_TIMEOUT_SECONDS = 25;
    private static final long POLL_MS = 500;
    // messages of recent challenges, so a screen that only got the message can still find the url
    private static final Map<String, String> blocked = Collections.synchronizedMap(new LinkedHashMap<String, String>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > 20;
        }
    });

    private Cloudflare() {
    }

    /**
     * The page to verify in the browser, when this error message came from a Cloudflare challenge.
     */
    @Nullable
    public static String blockedUrl(@Nullable String message) {
        return message == null ? null : blocked.get(message);
    }

    // the agent a blocked url was requested with, the browser has to use the same one
    private static final Map<String, String> agents = Collections.synchronizedMap(new LinkedHashMap<String, String>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > 20;
        }
    });

    @Nullable
    public static String userAgentFor(String url) {
        return url == null ? null : agents.get(url);
    }

    static boolean isChallenge(Response response) {
        int code = response.code();
        if (code != 403 && code != 503 && code != 429) return false;
        String server = response.header("Server", "");
        if (server == null || !server.toLowerCase().contains("cloudflare")) return false;
        if ("challenge".equalsIgnoreCase(response.header("cf-mitigated"))) return true;
        try {
            String body = response.peekBody(64 * 1024).string();
            return body.contains("challenge-platform") || body.contains("cf-browser-verification")
                    || body.contains("<title>Just a moment...</title>");
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * An application interceptor: retries a challenged request once the invisible WebView has
     * earned the clearance cookie.
     */
    public static class Interceptor implements okhttp3.Interceptor {
        private final Context context;

        public Interceptor(Context context) {
            this.context = context.getApplicationContext();
        }

        @NonNull
        @Override
        public Response intercept(@NonNull okhttp3.Interceptor.Chain chain) throws IOException {
            Request request = chain.request();
            Response response = chain.proceed(request);
            if (!isChallenge(response)) return response;
            response.close();

            String url = request.url().toString();
            String before = WebViewCookieJar.get(url, CLEARANCE_COOKIE);
            synchronized (Cloudflare.class) {
                // another request may have solved it while this one waited
                String now = WebViewCookieJar.get(url, CLEARANCE_COOKIE);
                if (now == null || now.equals(before)) {
                    solve(context, request, before);
                }
            }

            Response retried = chain.proceed(request.newBuilder().build());
            if (!isChallenge(retried)) return retried;
            retried.close();
            String agent = request.header("User-Agent");
            if (agent != null) agents.put(url, agent);
            throw challenge(context, url, request.url().host());
        }
    }

    private static ChallengeException challenge(Context context, String url, String host) {
        String message = context.getString(R.string.cloudflare_blocked, host);
        blocked.put(message, url);
        return new ChallengeException(message, url);
    }

    // blocks the calling (network) thread while a WebView on the main thread runs the challenge
    @SuppressLint("SetJavaScriptEnabled")
    private static void solve(Context context, Request request, String oldClearance) {
        if (Looper.myLooper() == Looper.getMainLooper()) return;
        String url = request.url().toString();
        String agent = request.header("User-Agent");
        CountDownLatch done = new CountDownLatch(1);
        Handler main = new Handler(Looper.getMainLooper());
        WebView[] holder = new WebView[1];

        main.post(() -> {
            try {
                WebView view = new WebView(context);
                holder[0] = view;
                WebSettings settings = view.getSettings();
                settings.setJavaScriptEnabled(true);
                settings.setDomStorageEnabled(true);
                if (agent != null) settings.setUserAgentString(agent);
                android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(view, true);
                view.setWebViewClient(new WebViewClient());
                view.loadUrl(url);

                Runnable poll = new Runnable() {
                    @Override
                    public void run() {
                        String clearance = WebViewCookieJar.get(url, CLEARANCE_COOKIE);
                        if (clearance != null && !clearance.equals(oldClearance)) {
                            done.countDown();
                        } else if (done.getCount() > 0) {
                            main.postDelayed(this, POLL_MS);
                        }
                    }
                };
                main.postDelayed(poll, POLL_MS);
            } catch (Throwable e) {
                // no WebView on this device
                done.countDown();
            }
        });

        try {
            done.await(SOLVE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            done.countDown();
            main.post(() -> {
                if (holder[0] == null) return;
                holder[0].stopLoading();
                holder[0].destroy();
            });
            try {
                android.webkit.CookieManager.getInstance().flush();
            } catch (Throwable ignored) {
                // no WebView
            }
        }
    }

    /**
     * The user agent for every request: the device's WebView agent, minus the WebView marker, so
     * it matches the browser engine that answers Cloudflare's checks.
     */
    @Nullable
    static String webViewUserAgent() {
        Context context = App.getContext();
        if (context == null) return null;
        try {
            String agent = WebSettings.getDefaultUserAgent(context);
            return agent.replace("; wv)", ")").replaceAll("Version/\\d+(\\.\\d+)*\\s", "");
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * A page kept back by a Cloudflare check that has to be passed by hand.
     */
    public static class ChallengeException extends IOException {
        public final String url;

        ChallengeException(String message, String url) {
            super(message);
            this.url = url;
        }
    }
}
