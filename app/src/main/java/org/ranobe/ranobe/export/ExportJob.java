package org.ranobe.ranobe.export;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.Novel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One novel being turned into an EPUB. Survives process death through {@link #toJson()}, the
 * fetched chapters live in the job's work folder so a resumed job only fetches what is missing.
 * <p>
 * The runner thread writes these fields and the UI reads them, hence the volatiles.
 */
public class ExportJob {
    public final String id;
    public final Novel novel;
    // inclusive chapter number range, NaN for open ends
    public final float from;
    public final float to;
    public final long createdAt;
    // filled once the chapter list is fetched, in reading order
    public final List<Item> chapters = new ArrayList<>();
    public volatile Status status = Status.QUEUED;
    public volatile Phase phase = Phase.CHAPTERS;
    // last error, why the job is paused, or what it is waiting for
    public volatile String message;
    public volatile boolean waitingForNetwork;
    // set by "build anyway", chapters that couldn't be fetched are left out
    public volatile boolean allowMissing;
    public volatile int buildDone;
    public volatile int buildTotal;
    // result
    public volatile String outputUri;
    public volatile String outputName;
    public volatile long outputSize;
    public volatile int missingChapters;
    public ExportJob(Novel novel, float from, float to) {
        this(UUID.randomUUID().toString(), novel, from, to, System.currentTimeMillis());
    }
    private ExportJob(String id, Novel novel, float from, float to, long createdAt) {
        this.id = id;
        this.novel = novel;
        this.from = from;
        this.to = to;
        this.createdAt = createdAt;
    }

    public static ExportJob fromJson(JSONObject o) throws JSONException {
        JSONObject n = o.getJSONObject("novel");
        Novel novel = new Novel(n.getString("url"), n.getInt("sourceId"));
        novel.name = n.optString("name", null);
        novel.cover = n.optString("cover", null);
        novel.summary = n.optString("summary", null);
        novel.authors = strings(n.optJSONArray("authors"));
        novel.genres = strings(n.optJSONArray("genres"));

        ExportJob job = new ExportJob(o.getString("id"), novel,
                (float) o.optDouble("from", Double.NaN), (float) o.optDouble("to", Double.NaN),
                o.optLong("createdAt"));
        job.status = Status.valueOf(o.getString("status"));
        job.phase = Phase.valueOf(o.optString("phase", Phase.CHAPTERS.name()));
        job.message = o.optString("message", null);
        job.allowMissing = o.optBoolean("allowMissing");
        job.outputUri = o.optString("outputUri", null);
        job.outputName = o.optString("outputName", null);
        job.outputSize = o.optLong("outputSize");
        job.missingChapters = o.optInt("missingChapters");
        JSONArray items = o.optJSONArray("chapters");
        if (items != null) {
            for (int i = 0; i < items.length(); i++)
                job.chapters.add(Item.fromJson(items.getJSONObject(i)));
        }
        return job;
    }

    private static List<String> strings(JSONArray array) {
        if (array == null) return null;
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) list.add(array.optString(i));
        return list;
    }

    public boolean isActive() {
        return status == Status.QUEUED || status == Status.RUNNING;
    }

    public boolean hasRange() {
        return !Float.isNaN(from) || !Float.isNaN(to);
    }

    public boolean inRange(float index) {
        return (Float.isNaN(from) || index >= from) && (Float.isNaN(to) || index <= to);
    }

    public int chaptersDone() {
        int done = 0;
        synchronized (chapters) {
            for (Item item : chapters) if (item.done) done++;
        }
        return done;
    }

    public int chapterCount() {
        synchronized (chapters) {
            return chapters.size();
        }
    }

    public int missingCount() {
        int missing = 0;
        synchronized (chapters) {
            for (Item item : chapters) if (item.done && item.missing) missing++;
        }
        return missing;
    }

    /**
     * Overall progress 0..100, the build step counts as the last 10%.
     */
    public int percent() {
        if (status == Status.COMPLETED) return 100;
        int total = chapterCount();
        if (total == 0) return 0;
        if (phase == Phase.BUILDING) {
            return 90 + (buildTotal == 0 ? 0 : buildDone * 10 / buildTotal);
        }
        return Math.min(90, chaptersDone() * 90 / total);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject n = new JSONObject()
                .put("url", novel.url)
                .put("name", novel.name)
                .put("cover", novel.cover)
                .put("summary", novel.summary)
                .put("sourceId", novel.sourceId);
        if (novel.authors != null) n.put("authors", new JSONArray(novel.authors));
        if (novel.genres != null) n.put("genres", new JSONArray(novel.genres));
        JSONArray items = new JSONArray();
        synchronized (chapters) {
            for (Item item : chapters) items.put(item.toJson());
        }
        return new JSONObject()
                .put("id", id)
                .put("novel", n)
                .put("from", Float.isNaN(from) ? null : (double) from)
                .put("to", Float.isNaN(to) ? null : (double) to)
                .put("createdAt", createdAt)
                .put("status", status.name())
                .put("phase", phase.name())
                .put("message", message)
                .put("allowMissing", allowMissing)
                .put("outputUri", outputUri)
                .put("outputName", outputName)
                .put("outputSize", outputSize)
                .put("missingChapters", missingChapters)
                .put("chapters", items);
    }

    public enum Status {
        QUEUED, RUNNING, PAUSED,
        // every chapter was tried but some couldn't be fetched, the user picks retry or build anyway
        INCOMPLETE,
        FAILED, COMPLETED
    }

    public enum Phase {CHAPTERS, CONTENT, BUILDING}

    /**
     * A chapter of the job, {@code done} once it was fetched or given up on.
     */
    public static class Item {
        public final String url;
        public final String name;
        public final float index;
        public volatile boolean missing;
        public volatile boolean done;

        Item(String url, String name, float index) {
            this.url = url;
            this.name = name;
            this.index = index;
        }

        static Item of(Chapter c) {
            return new Item(c.url, c.name, c.id);
        }

        static Item fromJson(JSONObject o) throws JSONException {
            Item item = new Item(o.getString("url"), o.optString("name", null), (float) o.getDouble("index"));
            item.missing = o.optBoolean("missing");
            item.done = o.optBoolean("done");
            return item;
        }

        Chapter toChapter(Novel novel) {
            Chapter c = new Chapter(novel.url);
            c.url = url;
            c.name = name;
            c.id = index;
            return c;
        }

        JSONObject toJson() throws JSONException {
            return new JSONObject()
                    .put("url", url)
                    .put("name", name)
                    .put("index", (double) index)
                    .put("missing", missing)
                    .put("done", done);
        }
    }
}
