package org.ranobe.downloader.config;

public class Config {
    private Config() throws IllegalAccessException {
        throw new IllegalAccessException("Cannot initialize this class ;)");
    }

    public static final String KEY_URL = "downloader-url";
    public static final String KEY_KEYWORD = "downloader-keyword";

}
