package com.aurora.cache.client;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CacheClient {
    private final HttpClient httpClient;
    private final String baseUrl;
    private final ObjectMapper mapper = new ObjectMapper();
    private final int gzipThreshold;

    public CacheClient(String baseUrl) {
        this(baseUrl, 0, HttpClient.newHttpClient());
    }

    public CacheClient(String baseUrl, int gzipThreshold) {
        this(baseUrl, gzipThreshold, HttpClient.newHttpClient());
    }

    public CacheClient(String baseUrl, int gzipThreshold, HttpClient httpClient) {
        if (baseUrl.endsWith("/")) {
            this.baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        } else {
            this.baseUrl = baseUrl;
        }
        this.gzipThreshold = gzipThreshold;
        this.httpClient = httpClient;
    }

    public <T> T get(String key, Class<T> clazz) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/cache/" + key))
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
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Unexpected response code " + response.statusCode());
        }
        java.util.Map<String, ?> raw = mapper.readValue(response.body(), new TypeReference<java.util.Map<String, Object>>() {});
        java.util.Map<String, T> result = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, ?> e : raw.entrySet()) {
            result.put(e.getKey(), mapper.convertValue(e.getValue(), clazz));
        }
        return result;
    }

    public void put(String key, Object value) throws IOException, InterruptedException {
        String json = mapper.writeValueAsString(java.util.Map.of("key", key, "value", value));
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/cache"))
                .header("Content-Type", "application/json");
        HttpRequest request = builder
                .POST(bodyPublisher(json, builder))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new IOException("Unexpected response code " + response.statusCode());
        }
    }

    public void putAll(java.util.Map<String, ?> entries) throws IOException, InterruptedException {
        String json = mapper.writeValueAsString(entries);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/cache/all"))
                .header("Content-Type", "application/json");
        HttpRequest request = builder
                .POST(bodyPublisher(json, builder))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new IOException("Unexpected response code " + response.statusCode());
        }
    }

    public void evict(String key) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/cache/" + key))
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
                .DELETE()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 204) {
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
