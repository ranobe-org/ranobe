package org.ranobe.ranobe.models;

import androidx.annotation.NonNull;

public class DataSource {
    public int sourceId;
    public String url;
    public String name;
    public String lang;
    public String logo;
    public String dev;
    public Boolean isActive;
    // false for sources whose site has no search, they get no search tab
    public boolean isSearchable = true;
    // recently added, Explore shows a "new" badge until the source is tapped once
    public boolean isNew = false;
    // the source is run by the app's developer, its details page offers to read on the site instead
    public boolean hasOwnSite = false;

    @NonNull
    @Override
    public String toString() {
        return "DataSource{" +
                "sourceId=" + sourceId +
                ", url='" + url + '\'' +
                '}';
    }
}
