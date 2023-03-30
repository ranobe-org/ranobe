package org.ranobe.ranobe.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;

import org.ranobe.ranobe.util.SourceUtils;

@Entity
public class Chapter extends ChapterItem {
    public String content;
    public int reads;

    public Chapter() {
        super();
    }

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
