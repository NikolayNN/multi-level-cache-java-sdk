package com.aurora.cache.client.model.impl;

import com.aurora.cache.client.model.CacheEntryRef;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CacheEntry<T>(@JsonProperty("c") String cacheName,
                         @JsonProperty("k") String key,
                         @JsonProperty("v") T value) implements CacheEntryRef {

    @Override
    public T value() {
        return value;
    }
}
