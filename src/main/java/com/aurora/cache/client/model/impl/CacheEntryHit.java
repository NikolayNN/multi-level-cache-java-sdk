package com.aurora.cache.client.model.impl;

import com.aurora.cache.client.model.CacheEntryHitRef;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CacheEntryHit<T>(@JsonProperty("c") String cacheName,
                               @JsonProperty("k") String key,
                               @JsonProperty("v") T value,
                               @JsonProperty("f") boolean found) implements CacheEntryHitRef {
}
