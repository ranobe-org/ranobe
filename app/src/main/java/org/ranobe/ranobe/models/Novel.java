package org.ranobe.ranobe.models;

import androidx.annotation.NonNull;

import java.util.List;

public class Novel extends NovelItem {
    public String status;
    public String summary;
    public List<String> alternateNames;
    public List<String> authors;
    public List<String> genres;
    public float rating;
    public int year;

    public Novel() {
    }

    @NonNull
    @Override
    public String toString() {
        return "Novel{" +
                "status='" + status + '\'' +
                ", summary='" + summary + '\'' +
                ", alternateNames=" + alternateNames +
                ", authors=" + authors +
                ", genres=" + genres +
                ", rating=" + rating +
                ", year=" + year +
                ", id=" + id +
                ", sourceId=" + sourceId +
                ", name='" + name + '\'' +
                ", cover='" + cover + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
