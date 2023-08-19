package org.ranobe.core.models;

import java.util.HashMap;
import java.util.Objects;

public class Filter {
    public static final String FILTER_KEYWORD = "keyword";
    private final HashMap<String, String> params;

    public Filter() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Filter filter = (Filter) o;
        return Objects.equals(params, filter.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(params);
    }
}
