package com.aurora.cache.client;

import com.aurora.cache.client.model.impl.CacheEntry;
import com.aurora.cache.client.model.impl.CacheEntryHit;
import com.aurora.cache.client.model.impl.CacheId;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CacheClientTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private MockWebServer server;
    private CacheClient client;

    public record User(int id, String name) {
    }

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        client = new CacheClient(server.url("/").toString());
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void testGetAll() throws Exception {
        List<CacheEntryHit<User>> expectedHits = List.of(
                new CacheEntryHit<>("users", "1", new User(1, "Alice"), true),
                new CacheEntryHit<>("users", "2", null, false)
        );
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(mapper.writeValueAsString(expectedHits)));

        List<CacheId> givenCacheIds = List.of(
                new CacheId("users", "1"),
                new CacheId("users", "2")
        );

        List<CacheEntryHit<User>> actual = client.getAll(givenCacheIds, User.class);
        RecordedRequest request = server.takeRequest();

        assertEquals("/api/v1/cache/get_all", request.getPath());
        assertEquals("POST", request.getMethod());
        assertEquals("application/json", request.getHeader("Content-Type"));

        JSONAssert.assertEquals("""
                [{"c":"users","k":"1"},{"c":"users","k":"2"}]
                """, request.getBody().readUtf8(), true);

        assertEquals(expectedHits, actual);
    }

    @Test
    void testPutAll() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(204));

        List<CacheEntry<?>> entries = List.of(
                new CacheEntry<>("users", "1", new User(1, "Alice"))
        );

        client.putAll(entries);

        RecordedRequest request = server.takeRequest();

        assertEquals("/api/v1/cache/put_all", request.getPath());
        assertEquals("POST", request.getMethod());

        JSONAssert.assertEquals("""
                [{"c":"users","k":"1","v":{"id":1,"name":"Alice"}}]
                """, request.getBody().readUtf8(), true);
    }

    @Test
    void testEvictAll() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(204));

        List<CacheId> givenCacheIds = List.of(new CacheId("users", "1"));

        client.evictAll(givenCacheIds);

        RecordedRequest request = server.takeRequest();

        assertEquals("/api/v1/cache/evict_all", request.getPath());
        assertEquals("POST", request.getMethod());

        JSONAssert.assertEquals("""
                [{"c":"users","k":"1"}]
                """, request.getBody().readUtf8(), true);
    }

}
