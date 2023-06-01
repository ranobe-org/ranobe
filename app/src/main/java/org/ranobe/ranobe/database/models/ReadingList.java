package org.ranobe.ranobe.database.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity
public class ReadingList {
    @PrimaryKey()
    @NonNull
    public String chapterUrl;
    public String novelUrl;
    public int read;
    public Date created;

    public ReadingList() {
        chapterUrl = "";
    }

    @Ignore
    public ReadingList(@NonNull String chapterUrl, String novelUrl) {
        this.chapterUrl = chapterUrl;
        this.novelUrl = novelUrl;
        this.read = 1;
        this.created = new Date();
    }
}
