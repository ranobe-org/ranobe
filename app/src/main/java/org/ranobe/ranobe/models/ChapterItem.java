package org.ranobe.ranobe.models;

import androidx.annotation.NonNull;

import org.ranobe.ranobe.util.SourceUtils;

public class ChapterItem {
    public float id;
    public float novelId;
    public String name;
    public String updated;
    public String url;

    public ChapterItem(String novelUrl) {
        this.novelId = SourceUtils.generateId(novelUrl);
    }

    @NonNull
    @Override
    public String toString() {
        return "ChapterItem{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", updated='" + updated + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
