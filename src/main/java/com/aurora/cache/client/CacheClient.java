package com.aurora.cache.client;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CacheClient {
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

    public <T> T get(String key, Class<T> clazz) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/cache/" + key))
                .timeout(getTimeout)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return mapper.readValue(response.body(), clazz);
        }
        throw new IOException("Unexpected response code " + response.statusCode());
    }

    public <T> java.util.Map<String, T> getAll(Class<T> clazz) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/cache/all"))
                .timeout(getTimeout)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Unexpected response code " + response.statusCode());
        }
        return mapper.readValue(
                response.body(),
                mapper.getTypeFactory().constructMapType(Map.class, String.class, clazz));
    }

    public void put(String key, Object value) throws IOException, InterruptedException {
        String json = mapper.writeValueAsString(Map.of("key", key, "value", value));
        post(baseUrl + "/cache", json, putTimeout);
    }

    public void putAll(java.util.Map<String, ?> entries) throws IOException, InterruptedException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, ?> e : entries.entrySet()) {
            list.add(Map.of("key", e.getKey(), "value", e.getValue()));
        }
        String json = mapper.writeValueAsString(list);
        post(baseUrl + "/cache/all", json, putTimeout);
    }

    public void evict(String key) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/cache/" + key))
                .timeout(evictTimeout)
                .DELETE()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 204) {
            throw new IOException("Unexpected response code " + response.statusCode());
        }
    }

    public void evictAll() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/cache/all"))
                .timeout(evictTimeout)
                .DELETE()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 204) {
            throw new IOException("Unexpected response code " + response.statusCode());
        }
    }

    private void post(String url, String json, Duration timeout) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .header("Content-Type", "application/json");
        HttpRequest request = builder
                .POST(bodyPublisher(json, builder))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new IOException("Unexpected response code " + response.statusCode());
        }
    }

    private HttpRequest.BodyPublisher bodyPublisher(String json, HttpRequest.Builder builder) throws IOException {
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

    // no manual JSON helpers needed when using Jackson
}
