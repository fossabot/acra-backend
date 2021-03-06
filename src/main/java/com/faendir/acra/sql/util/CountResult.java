package com.faendir.acra.sql.util;

/**
 * @author Lukas
 * @since 19.12.2017
 */
public class CountResult<T> {
    T group;
    long count;

    public CountResult(T group, long count) {
        this.group = group;
        this.count = count;
    }

    public T getGroup() {
        return group;
    }

    public long getCount() {
        return count;
    }
}
