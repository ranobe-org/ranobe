package org.ranobe.ranobe.models;

import androidx.annotation.NonNull;

public class ChapterItem {
    public int id;
    public String name;
    public String updated;
    public String url;

    public ChapterItem(){}

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
