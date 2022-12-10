package org.ranobe.ranobe.models;

import androidx.annotation.NonNull;

public class Chapter extends ChapterItem {
    public String content;

    public Chapter() {
    }

    @NonNull
    @Override
    public String toString() {
        return "Chapter{" +
                "content='" + content + '\'' +
                ", id=" + id +
                ", name='" + name + '\'' +
                ", updated='" + updated + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
