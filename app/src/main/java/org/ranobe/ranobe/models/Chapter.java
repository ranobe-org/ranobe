package org.ranobe.ranobe.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity
public class Chapter implements Parcelable {
    @PrimaryKey
    @NonNull
    public String url;
    public String novelUrl;
    public String content;
    public String name;
    public String updated;
    public float id;

    public Chapter() {
        this.url = "";
    }

    @Ignore
    public Chapter(String novelUrl) {
        this.novelUrl = novelUrl;
        this.url = "";
    }

    @Ignore
    protected Chapter(Parcel in) {
        url = in.readString();
        novelUrl = in.readString();
        content = in.readString();
        name = in.readString();
        updated = in.readString();
        id = in.readFloat();
    }

    public static final Creator<Chapter> CREATOR = new Creator<Chapter>() {
        @Override
        public Chapter createFromParcel(Parcel in) {
            return new Chapter(in);
        }

        @Override
        public Chapter[] newArray(int size) {
            return new Chapter[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        return "Chapter{" +
                "url='" + url + '\'' +
                ", content='" + content + '\'' +
                ", name='" + name + '\'' +
                ", updated='" + updated + '\'' +
                ", id=" + id +
                ", novelUrl=" + novelUrl +
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
    }
}
