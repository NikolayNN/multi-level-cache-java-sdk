package com.aurora.cache.client;

import com.aurora.cache.client.model.impl.CacheEntry;
import com.aurora.cache.client.model.impl.CacheEntryHit;
import com.aurora.cache.client.model.impl.CacheId;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class CacheClient {

    private static final String ENDPOINT_GET_ALL = "/api/v1/cache/get_all";
    private static final String ENDPOINT_PUT_ALL = "/api/v1/cache/put_all";
    private static final String ENDPOINT_EVICT_ALL = "/api/v1/cache/evict_all";

    private final HttpClient httpClient;
    private final String baseUrl;
    private final ObjectMapper mapper = new ObjectMapper();
    private final int gzipThreshold;
    private final Duration getTimeout;
    private final Duration putTimeout;
    private final Duration evictTimeout;

    public CacheClient(String baseUrl) {
        this(baseUrl, 0);
    }

    public CacheClient(String baseUrl, int gzipThreshold) {
        this(baseUrl, gzipThreshold,
                Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofSeconds(5));
    }

    public CacheClient(String baseUrl, int gzipThreshold,
                       Duration getTimeout, Duration putTimeout, Duration evictTimeout) {
        this(baseUrl, gzipThreshold, getTimeout, putTimeout, evictTimeout,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    public CacheClient(String baseUrl, int gzipThreshold,
                       Duration getTimeout, Duration putTimeout, Duration evictTimeout,
                       HttpClient httpClient) {
        if (baseUrl.endsWith("/")) {
            this.baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        } else {
            this.baseUrl = baseUrl;
        }
        this.gzipThreshold = gzipThreshold;
        this.httpClient = httpClient;
        this.getTimeout = getTimeout;
        this.putTimeout = putTimeout;
        this.evictTimeout = evictTimeout;
    }

    public <T> List<CacheEntryHit<T>> getAll(Collection<CacheId> cacheIds, Class<T> clazz) throws IOException, InterruptedException {
        String requestBody = mapper.writeValueAsString(cacheIds);
        HttpResponse<String> response = sendRequest(ENDPOINT_GET_ALL, getTimeout, requestBody);
        JavaType type = mapper.getTypeFactory()
                .constructCollectionType(List.class,
                        mapper.getTypeFactory().constructParametricType(CacheEntryHit.class, clazz));
        return mapper.readValue(response.body(), type);
    }


    public void putAll(Collection<? extends CacheEntry<?>> entries) throws IOException, InterruptedException {
        String requestBody = mapper.writeValueAsString(entries);
        sendRequest(ENDPOINT_PUT_ALL, putTimeout, requestBody);
    }

    public void evictAll(Collection<CacheId> cacheIds) throws IOException, InterruptedException {
        String requestBody = mapper.writeValueAsString(cacheIds);
        sendRequest(ENDPOINT_EVICT_ALL, evictTimeout, requestBody);
    }

    public HttpResponse<String> sendRequest(String endpoint, Duration timeout, String requestBody) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .timeout(timeout)
                .header("Content-Type", "application/json");

        HttpRequest request = builder
                .POST(gzipBodyPublisher(requestBody, builder))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200 && response.statusCode() != 204) {
            throw new IOException("Unexpected response code " + response.statusCode() + ": " + response.body());
        }

        return response;
    }

    private HttpRequest.BodyPublisher gzipBodyPublisher(String json, HttpRequest.Builder builder) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        if (gzipThreshold > 0 && bytes.length >= gzipThreshold) {
            builder.header("Content-Encoding", "gzip");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
                gzip.write(bytes);
            }
            return HttpRequest.BodyPublishers.ofByteArray(out.toByteArray());
        }
        return HttpRequest.BodyPublishers.ofByteArray(bytes);
    }
}
