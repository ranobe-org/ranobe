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
                @Index(value = {"novelUrl", "timestamp"}, orders = {Index.Order.ASC, Index.Order.DESC}),
                @Index(value = "url", unique = true)
        }
)
public class ReadHistory implements Parcelable {
    public static final Creator<ReadHistory> CREATOR = new Creator<ReadHistory>() {
        @Override
        public ReadHistory createFromParcel(Parcel in) {
            return new ReadHistory(in);
        }

        @Override
        public ReadHistory[] newArray(int size) {
            return new ReadHistory[size];
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
    public int position;
    public int readerOffset;
    public long timestamp;
    public String cover;
    public String novelName;
    public int sourceId;

    public ReadHistory() {
        this.url = "";
    }

    @Ignore
    public ReadHistory(String novelUrl) {
        this.novelUrl = novelUrl;
        this.url = "";
    }

    @Ignore
    protected ReadHistory(Parcel in) {
        url = in.readString();
        novelUrl = in.readString();
        content = in.readString();
        name = in.readString();
        updated = in.readString();
        id = in.readFloat();
        position = in.readInt();
        readerOffset = in.readInt();
        timestamp = in.readLong();
        cover   = in.readString();
        novelName   = in.readString();
        sourceId = in.readInt();
    }

    @NonNull
    @Override
    public String toString() {
        return "ReadHistory{" +
                "url='" + url + '\'' +
                ", content='" + content + '\'' +
                ", name='" + name + '\'' +
                ", updated='" + updated + '\'' +
                ", id=" + id +
                ", novelUrl=" + novelUrl +
                ", position=" + position +
                ", readerOffset=" + readerOffset +
                ", timestamp=" + timestamp +
                ", cover=" + cover +
                ", novelName=" + novelName +
                ", sourceId=" + sourceId +
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
        parcel.writeInt(position);
        parcel.writeInt(readerOffset);
        parcel.writeLong(timestamp);
        parcel.writeString(cover);
        parcel.writeString(novelName);
        parcel.writeInt(sourceId);

    }
}
