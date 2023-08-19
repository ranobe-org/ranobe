package org.ranobe.core.models;


public class DataSource {
    public int sourceId;
    public String url;
    public String name;
    public String lang;
    public String logo;
    public String dev;

    @Override
    public String toString() {
        return "DataSource{" +
                "sourceId=" + sourceId +
                ", url='" + url + '\'' +
                '}';
    }
}
