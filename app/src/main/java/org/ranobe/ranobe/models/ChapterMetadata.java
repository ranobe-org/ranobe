package org.ranobe.ranobe.models;


import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        indices = {
                @Index(value = "novelUrl"),
                @Index(value = {"novelUrl", "id"})
        }
)
public class ChapterMetadata implements Parcelable {
    public static final Creator<ChapterMetadata> CREATOR = new Creator<ChapterMetadata>() {
        @Override
        public ChapterMetadata createFromParcel(Parcel in) {
            return new ChapterMetadata(in);
        }

        @Override
        public ChapterMetadata[] newArray(int size) {
            return new ChapterMetadata[size];
        }
    };
    @PrimaryKey
    @NonNull
    public String url;
    public String novelUrl;
    public String content;
    public String name;
    public String updated;
    public float id;
    public long cachedDate;

    public ChapterMetadata() {
        this.url = "";
    }

    @Ignore
    public ChapterMetadata(String novelUrl) {
        this.novelUrl = novelUrl;
        this.url = "";
    }

    @Ignore
    protected ChapterMetadata(Parcel in) {
        url = in.readString();
        novelUrl = in.readString();
        content = in.readString();
        name = in.readString();
        updated = in.readString();
        id = in.readFloat();
        cachedDate = in.readLong();
    }

    @NonNull
    @Override
    public String toString() {
        return "ChapterMetadata{" +
                "url='" + url + '\'' +
                ", content='" + content + '\'' +
                ", name='" + name + '\'' +
                ", updated='" + updated + '\'' +
                ", id=" + id +
                ", novelUrl=" + novelUrl +
                ", cachedDate=" + cachedDate +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(url);
        parcel.writeString(novelUrl);
        parcel.writeString(content);
        parcel.writeString(name);
        parcel.writeString(updated);
        parcel.writeFloat(id);
        parcel.writeLong(cachedDate);
    }
}
