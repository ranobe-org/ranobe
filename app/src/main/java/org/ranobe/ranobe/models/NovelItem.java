package org.ranobe.ranobe.models;

import androidx.annotation.NonNull;

public class NovelItem {
    public int id;
    public int sourceId;
    public String name;
    public String cover;
    public String url;

    public NovelItem() {}

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
