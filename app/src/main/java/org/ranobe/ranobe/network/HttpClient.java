package org.ranobe.ranobe.network;

import android.content.Context;

import androidx.annotation.NonNull;

import org.ranobe.ranobe.App;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.ConnectionPool;
import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HttpClient {
    // okhttp's default "okhttp/x.y" agent gets challenged or throttled by most novel sites
    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36";
    private static volatile OkHttpClient client = null;

    private static OkHttpClient client() {
        if (client == null) {
            synchronized (HttpClient.class) {
                if (client != null) return client;
                Context context = App.getContext();
                String tmp = System.getProperty("java.io.tmpdir");
                File baseDir = context != null ? context.getCacheDir() : new File(tmp != null ? tmp : ".");
                File cacheDir = new File(baseDir, "cache-files");
                Cache cache = new Cache(cacheDir, 50 * 1024 * 1024); //50 MiB, chapter pages are large
                client = new OkHttpClient
                        .Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        // keep sockets alive across screens so repeat requests to a source skip TLS setup
                        .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
                        .addInterceptor(new DefaultHeadersInterceptor())
                        .addInterceptor(new OfflineCacheInterceptor())
                        .addNetworkInterceptor(new CacheInterceptor())
                        .cache(cache)
                        .build();
            }
        }
        return client;
    }

    public static String GET(String url, HashMap<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder().url(url);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.addHeader(entry.getKey(), entry.getValue());
        }
        try (Response response = HttpClient.client().newCall(builder.build()).execute()) {
            ResponseBody body = response.body();
            return body == null ? "" : body.string();
        }
    }

    public static void DOWNLOAD(String url, HashMap<String, String> headers, File dest) throws IOException {
        Request.Builder builder = new Request.Builder().url(url).cacheControl(CacheControl.FORCE_NETWORK);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.addHeader(entry.getKey(), entry.getValue());
        }
        try (Response response = HttpClient.client().newCall(builder.build()).execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new IOException("HTTP " + response.code() + " for " + url);
            }
            try (InputStream in = body.byteStream(); OutputStream out = new FileOutputStream(dest)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
        }
    }

    public static String POST(String url, HashMap<String, String> headers, HashMap<String, String> form) throws IOException {
        Request.Builder builder = new Request.Builder().url(url);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.addHeader(entry.getKey(), entry.getValue());
        }
        FormBody.Builder formBody = new FormBody.Builder();
        for (Map.Entry<String, String> entry : form.entrySet()) {
            formBody.add(entry.getKey(), entry.getValue());
        }
        try (Response response = HttpClient.client().newCall(builder.post(formBody.build()).build()).execute()) {
            ResponseBody body = response.body();
            return body == null ? "" : body.string();
        }
    }

    public static class DefaultHeadersInterceptor implements Interceptor {
        @NonNull
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            if (request.header("User-Agent") != null) return chain.proceed(request);
            return chain.proceed(request.newBuilder().header("User-Agent", USER_AGENT).build());
        }
    }

    // when the network is down or flaky, fall back to whatever copy is in the disk cache
    public static class OfflineCacheInterceptor implements Interceptor {
        @NonNull
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            try {
                return chain.proceed(request);
            } catch (IOException e) {
                if (!"GET".equals(request.method())) throw e;
                CacheControl staleOk = new CacheControl.Builder()
                        .onlyIfCached()
                        .maxStale(7, TimeUnit.DAYS)
                        .build();
                Response cached = chain.proceed(request.newBuilder().cacheControl(staleOk).build());
                if (cached.isSuccessful()) return cached;
                cached.close();
                throw e;
            }
        }
    }

    public static class CacheInterceptor implements Interceptor {
        @NonNull
        @Override
        public Response intercept(Chain chain) throws IOException {
            Response response = chain.proceed(chain.request());

            // never pin error / challenge pages in the cache, they'd be served for the next 15 minutes
            if (!response.isSuccessful()) return response;

            CacheControl cacheControl = new CacheControl.Builder()
                    .maxAge(15, TimeUnit.MINUTES) // 15 minutes cache
                    .build();

            return response.newBuilder()
                    .removeHeader("Pragma")
                    .removeHeader("Cache-Control")
                    .header("Cache-Control", cacheControl.toString())
                    .build();
        }
    }
}
