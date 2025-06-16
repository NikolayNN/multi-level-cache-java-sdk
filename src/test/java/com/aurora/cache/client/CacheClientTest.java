package com.aurora.cache.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CacheClientTest {
    private HttpClient httpClient;
    private CacheClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        client = new CacheClient(
                "http://localhost", 5,
                Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1),
                httpClient);
    }

    @Test
    void testPutAndGet() throws Exception {
        HttpResponse<String> putResp = mockResponse(200, "{}");
        HttpResponse<String> getResp = mockResponse(200, "\"b\"");
        when(httpClient.send(any(), any())).thenReturn(putResp, getResp);

        client.put("a", "b");
        String val = client.get("a", String.class);
        assertEquals("b", val);

        verify(httpClient, times(2)).send(any(), any());
    }

    @Test
    void testPutAllAndGetAll() throws Exception {
        HttpResponse<String> putResp = mockResponse(200, "{}");
        String body = mapper.writeValueAsString(Map.of("a", "1", "b", "2"));
        HttpResponse<String> getResp = mockResponse(200, body);
        when(httpClient.send(any(), any())).thenReturn(putResp, getResp);

        client.putAll(Map.of("a", "1", "b", "2"));
        Map<String, String> res = client.getAll(String.class);
        assertEquals(Map.of("a", "1", "b", "2"), res);
    }

    @Test
    void testEvict() throws Exception {
        HttpResponse<String> putResp = mockResponse(200, "{}");
        HttpResponse<String> evictResp = mockResponse(200, "{}");
        HttpResponse<String> getResp = mockResponse(404, "");
        when(httpClient.send(any(), any())).thenReturn(putResp, evictResp, getResp);

        client.put("a", "b");
        client.evict("a");
        assertThrows(IOException.class, () -> client.get("a", String.class));
    }

    @Test
    void testEvictAll() throws Exception {
        HttpResponse<String> putResp = mockResponse(200, "{}");
        HttpResponse<String> evictResp = mockResponse(200, "{}");
        HttpResponse<String> getResp = mockResponse(200, "{}");
        when(httpClient.send(any(), any())).thenReturn(putResp, evictResp, getResp);

        client.putAll(Map.of("a", "1"));
        client.evictAll();
        Map<String, String> res = client.getAll(String.class);
        assertTrue(res.isEmpty());
    }

    @Test
    void testGzipCompression() throws Exception {
        HttpResponse<String> putResp = mockResponse(200, "{}");
        HttpResponse<String> getResp = mockResponse(200, "\"0123456789\"");
        when(httpClient.send(any(), any())).thenReturn(putResp, getResp);

        client.put("long", "0123456789");
        String val = client.get("long", String.class);
        assertEquals("0123456789", val);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(captor.capture(), any());
        HttpRequest putReq = captor.getAllValues().get(0);
        assertEquals("gzip", putReq.headers().firstValue("Content-Encoding").orElse(""));
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> mockResponse(int code, String body) {
        HttpResponse<String> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(code);
        when(resp.body()).thenReturn(body);
        return resp;
    }
}
