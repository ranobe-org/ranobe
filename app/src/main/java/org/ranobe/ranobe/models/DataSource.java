package org.ranobe.ranobe.models;

import androidx.annotation.NonNull;

public class DataSource {
    public int sourceId;
    public String url;
    public String name;
    public String lang;
    public String logo;
    public String dev;

    @NonNull
    @Override
    public String toString() {
        return "DataSource{" +
                "sourceId=" + sourceId +
                ", url='" + url + '\'' +
                '}';
    }
}
