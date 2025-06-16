package com.aurora.cache.client.model;

public interface CacheEntryRef<T> extends CacheIdRef {
    T value();
}
