package org.ranobe.ranobe.models;

import androidx.annotation.NonNull;

import org.ranobe.ranobe.util.SourceUtils;

public class Chapter extends ChapterItem {
    public String content;

    public Chapter(String novelUrl) {
        super(novelUrl);
        this.novelId = SourceUtils.generateId(novelUrl);
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
