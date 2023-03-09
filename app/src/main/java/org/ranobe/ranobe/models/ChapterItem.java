package org.ranobe.ranobe.models;

import androidx.annotation.NonNull;
import androidx.room.PrimaryKey;

import org.ranobe.ranobe.util.SourceUtils;

public class ChapterItem {
    public float id;
    public long novelId;
    public String name;
    public String updated;
    @PrimaryKey
    @NonNull
    public String url;

    public ChapterItem() {
        this.url = "";
    }

    public ChapterItem(String novelUrl) {
        this.novelId = SourceUtils.generateId(novelUrl);
        this.url = "";
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
