package com.aurora.cache.client.model.impl;

import com.aurora.cache.client.model.CacheIdRef;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CacheId(@JsonProperty("c") String cacheName,
                      @JsonProperty("k") String key) implements CacheIdRef {

}
