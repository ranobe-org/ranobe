package org.ranobe.ranobe.database.mapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.models.NovelMetadata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NovelMapper {

    public static Novel ToNovel(NovelMetadata data) {
        if (data == null) return null;

        Novel novel = new Novel(data.url);
        novel.id = data.id;
        novel.sourceId = data.sourceId;
        novel.name = data.name;
        novel.cover = data.cover;
        novel.status = data.status;
        novel.summary = data.summary;
        novel.alternateNames = data.alternateNames != null ? new ArrayList<>(data.alternateNames) : new ArrayList<>();
        novel.authors = data.authors != null ? new ArrayList<>(data.authors) : new ArrayList<>();
        novel.genres = data.genres != null ? new ArrayList<>(data.genres) : new ArrayList<>();
        novel.rating = data.rating;
        novel.year = data.year;
        novel.recommendations = recommendationsFromJson(data.recommendations);
        novel.genreLinks = linksFromJson(data.genreLinks);

        return novel;
    }

    public static NovelMetadata ToNovelMetadata(Novel novel) {
        if (novel == null) return null;

        NovelMetadata metadata = new NovelMetadata(novel.url);
        metadata.id = novel.id;
        metadata.sourceId = novel.sourceId;
        metadata.name = novel.name;
        metadata.cover = novel.cover;
        metadata.status = novel.status;
        metadata.summary = novel.summary;
        metadata.alternateNames = novel.alternateNames != null ? new ArrayList<>(novel.alternateNames) : new ArrayList<>();
        metadata.authors = novel.authors != null ? new ArrayList<>(novel.authors) : new ArrayList<>();
        metadata.genres = novel.genres != null ? new ArrayList<>(novel.genres) : new ArrayList<>();
        metadata.rating = novel.rating;
        metadata.year = novel.year;
        metadata.recommendations = recommendationsToJson(novel.recommendations);
        metadata.genreLinks = novel.genreLinks == null || novel.genreLinks.isEmpty() ? null : new JSONObject(novel.genreLinks).toString();

        return metadata;
    }

    // only what a cover tile needs, the rest is fetched when one is opened
    static String recommendationsToJson(List<Novel> items) {
        if (items == null || items.isEmpty()) return null;
        JSONArray array = new JSONArray();
        try {
            for (Novel item : items) {
                array.put(new JSONObject()
                        .put("url", item.url)
                        .put("name", item.name)
                        .put("cover", item.cover)
                        .put("sourceId", item.sourceId));
            }
        } catch (JSONException e) {
            return null;
        }
        return array.toString();
    }

    static Map<String, String> linksFromJson(String json) {
        Map<String, String> links = new LinkedHashMap<>();
        if (json == null || json.isEmpty()) return links;
        try {
            JSONObject o = new JSONObject(json);
            Iterator<String> keys = o.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                links.put(key, o.getString(key));
            }
        } catch (JSONException e) {
            links.clear();
        }
        return links;
    }

    static List<Novel> recommendationsFromJson(String json) {
        List<Novel> items = new ArrayList<>();
        if (json == null || json.isEmpty()) return items;
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                Novel item = new Novel(o.getString("url"), o.optInt("sourceId"));
                item.name = o.optString("name", null);
                item.cover = o.optString("cover", null);
                items.add(item);
            }
        } catch (JSONException e) {
            // a broken cache entry just shows no suggestions
            items.clear();
        }
        return items;
    }

}
