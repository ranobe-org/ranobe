package org.ranobe.ranobe.repository;

import org.json.JSONObject;
import org.ranobe.ranobe.BuildConfig;
import org.ranobe.ranobe.network.HttpClient;
import org.ranobe.ranobe.util.Version;

import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GithubRepo {
    private static final String API_ENDPOINT = "https://api.github.com/repos/ranobe-org/ranobe/releases/latest";
    private final HashMap<String, String> HEADERS = new HashMap<String, String>() {{
        put("Accept", "application/vnd.github+json");
    }};
    private final Executor executor;

    public GithubRepo() {
        this.executor = Executors.newCachedThreadPool();
    }

    public void getLatestRelease(Callback<GithubRelease> callback) {
        this.executor.execute(() -> {
            try {
                GithubRelease release = fetchLatestRelease();
                callback.onComplete(release);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    private GithubRelease fetchLatestRelease() throws Exception {
        String json = HttpClient.GET(API_ENDPOINT, HEADERS);
        JSONObject response = new JSONObject(json);

        String tag = response.getString("tag_name");
        Version latestVersion = new Version(Version.extractVersionNumber(tag));
        Version currentVersion = new Version(Version.extractVersionNumber(BuildConfig.VERSION_NAME));

        if (latestVersion.get().equals(currentVersion.get())) {
            return new GithubRelease(false, currentVersion.get(), null);
        }

        if (latestVersion.compareTo(currentVersion) > 0) {
            String url = response.getString("html_url");
            return new GithubRelease(true, tag, url);
        }

        return new GithubRelease(false, currentVersion.get(), null);
    }

    public interface Callback<T> {
        void onComplete(T result);

        void onError(Exception e);
    }

    public static class GithubRelease {
        public boolean updateAvailable;
        public String newReleaseVersion;
        public String newReleaseUrl;

        public GithubRelease(boolean updateAvailable, String newReleaseVersion, String newReleaseUrl) {
            this.updateAvailable = updateAvailable;
            this.newReleaseVersion = newReleaseVersion;
            this.newReleaseUrl = newReleaseUrl;
        }
    }
}
