package org.ranobe.ranobe.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import org.ranobe.ranobe.util.SourceUtils;

import java.util.List;

@Entity(
        indices = {
                @Index(value = "name"),
                @Index(value = "sourceId"),
                @Index(value = {"name", "sourceId"}),
                @Index(value = "url")
        }
)
public class NovelMetadata implements Parcelable {
    public static final Creator<NovelMetadata> CREATOR = new Creator<NovelMetadata>() {
        @Override
        public NovelMetadata createFromParcel(Parcel in) {
            return new NovelMetadata(in);
        }

        @Override
        public NovelMetadata[] newArray(int size) {
            return new NovelMetadata[size];
        }
    };
    @PrimaryKey
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
    public long cachedDate;

    public NovelMetadata(String url) {
        this.id = SourceUtils.generateId(url);
        this.url = url;
    }

    protected NovelMetadata(Parcel in) {
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
        cachedDate = in.readLong();
    }

    @NonNull
    @Override
    public String toString() {
        return "NovelMetadata{" +
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
                ", cachedDate=" + cachedDate +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
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
        parcel.writeLong(cachedDate);
    }
}
