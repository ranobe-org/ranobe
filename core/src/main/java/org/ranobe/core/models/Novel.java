package org.ranobe.core.models;


import android.os.Parcel;
import android.os.Parcelable;


import org.ranobe.core.util.SourceUtils;

import java.util.List;

public class Novel implements Parcelable {
    public long id;
    public int sourceId;
    public String name;
    public String cover;
    public String url;

    public String status;
    public String summary;
    public List<String> alternateNames;
    public List<String> authors;
    public List<String> genres;
    public float rating;
    public int year;

    public Novel(String url) {
        this.id = SourceUtils.generateId(url);
        this.url = url;
    }

    protected Novel(Parcel in) {
        id = in.readLong();
        sourceId = in.readInt();
        name = in.readString();
        cover = in.readString();
        url = in.readString();
        status = in.readString();
        summary = in.readString();
        alternateNames = in.createStringArrayList();
        authors = in.createStringArrayList();
        genres = in.createStringArrayList();
        rating = in.readFloat();
        year = in.readInt();
    }

    public static final Creator<Novel> CREATOR = new Creator<Novel>() {
        @Override
        public Novel createFromParcel(Parcel in) {
            return new Novel(in);
        }

        @Override
        public Novel[] newArray(int size) {
            return new Novel[size];
        }
    };

    @Override
    public String toString() {
        return "Novel{" +
                "id=" + id +
                ", sourceId=" + sourceId +
                ", name='" + name + '\'' +
                ", cover='" + cover + '\'' +
                ", url='" + url + '\'' +
                ", status='" + status + '\'' +
                ", summary='" + summary + '\'' +
                ", alternateNames=" + alternateNames +
                ", authors=" + authors +
                ", genres=" + genres +
                ", rating=" + rating +
                ", year=" + year +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeInt(sourceId);
        parcel.writeString(name);
        parcel.writeString(cover);
        parcel.writeString(url);
        parcel.writeString(status);
        parcel.writeString(summary);
        parcel.writeStringList(alternateNames);
        parcel.writeStringList(authors);
        parcel.writeStringList(genres);
        parcel.writeFloat(rating);
        parcel.writeInt(year);
    }
}
