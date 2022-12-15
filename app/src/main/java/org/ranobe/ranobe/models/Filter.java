package org.ranobe.ranobe.models;

import java.util.HashMap;

public class Filter {
    public static final String FILTER_KEYWORD = "keyword";
    private final HashMap<String, String> params;

    public Filter(){
        params = new HashMap<>();
    }

    public void addFilter(String key, String val) {
        params.put(key, val);
    }

    public boolean hashKeyword() {
        String val = params.get(FILTER_KEYWORD);
        return val != null;
    }

    public String getKeyword() {
        return params.get(FILTER_KEYWORD);
    }
}
