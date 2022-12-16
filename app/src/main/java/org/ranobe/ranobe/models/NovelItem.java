package org.ranobe.ranobe.models;

import androidx.annotation.NonNull;

import org.ranobe.ranobe.util.SourceUtils;

public class NovelItem {
    public long id;
    public int sourceId;
    public String name;
    public String cover;
    public String url;

    public NovelItem(String url) {
        this.id = SourceUtils.generateId(url);
        this.url = url;
    }

    @NonNull
    @Override
    public String toString() {
        return "NovelItem{" +
                "id=" + id +
                ", sourceId=" + sourceId +
                ", name='" + name + '\'' +
                ", cover='" + cover + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
