package org.ranobe.ranobe.models;

import androidx.annotation.NonNull;

public class DataSource {
    public int sourceId;
    public String url;

    @NonNull
    @Override
    public String toString() {
        return "DataSource{" +
                "sourceId=" + sourceId +
                ", url='" + url + '\'' +
                '}';
    }
}
