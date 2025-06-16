package com.aurora.cache.client.model;

public interface CacheEntryHitRef<T> extends CacheEntryRef<T> {
    boolean found();
}
