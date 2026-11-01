package io.github.adrish_s.confluence.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.adrish_s.confluence.auth.AuthenticationStrategy;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpConfluenceGateway implements ConfluenceGateway {
    private final String baseUrl;
    private final AuthenticationStrategy authStrategy;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final Map<String, String> spaceKeyToIdCache = new ConcurrentHashMap<>();

    public HttpConfluenceGateway(String baseUrl, AuthenticationStrategy authStrategy) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.authStrategy = authStrategy;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.mapper = new ObjectMapper();
    }

    @Override
    public String upsertPage(ConfluencePageRequest req) {
        try {
            String spaceId = resolveSpaceId(req.getSpaceKey());
            PageRecord existing = findPage(req.getTitle(), spaceId);

            if (existing == null) {
                return createPage(req.getTitle(), spaceId, req.getParentId(), req.getXhtmlContent());
            } else {
                updatePage(existing.id, req.getTitle(), existing.version + 1, req.getXhtmlContent());
                return existing.id;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to upsert Confluence page: " + req.getTitle(), e);
        }
    }

    private String resolveSpaceId(String spaceKey) throws IOException, InterruptedException {
        if (spaceKeyToIdCache.containsKey(spaceKey)) {
            return spaceKeyToIdCache.get(spaceKey);
        }

        String url = baseUrl + "/api/v2/spaces?keys=" + URLEncoder.encode(spaceKey, StandardCharsets.UTF_8);
        HttpResponse<String> response = execute(newGetBuilder(url));
        JsonNode root = mapper.readTree(response.body());
        JsonNode results = root.get("results");

        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException("No Confluence Space found for key: " + spaceKey);
        }

        String spaceId = results.get(0).get("id").asText();
        spaceKeyToIdCache.put(spaceKey, spaceId);
        return spaceId;
    }

    private static class PageRecord {
        final String id;
        final int version;
        PageRecord(String id, int version) { this.id = id; this.version = version; }
    }

    private PageRecord findPage(String title, String spaceId) throws IOException, InterruptedException {
        String url = baseUrl + "/api/v2/pages?title=" + URLEncoder.encode(title, StandardCharsets.UTF_8)
                + "&space-id=" + spaceId;
        HttpResponse<String> response = execute(newGetBuilder(url));
        JsonNode root = mapper.readTree(response.body());
        JsonNode results = root.get("results");

        if (results != null && !results.isEmpty()) {
            JsonNode item = results.get(0);
            return new PageRecord(item.get("id").asText(), item.get("version").get("number").asInt());
        }
        return null;
    }

    private String createPage(String title, String spaceId, String parentId, String xhtml) throws IOException, InterruptedException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("spaceId", spaceId);
        payload.put("status", "current");
        payload.put("title", title);
        if (parentId != null && !parentId.isBlank()) {
            payload.put("parentId", parentId);
        }
        payload.put("body", Map.of("representation", "storage", "value", xhtml));

        String json = mapper.writeValueAsString(payload);
        HttpRequest.Builder builder = newPostBuilder(baseUrl + "/api/v2/pages", json);
        HttpResponse<String> response = execute(builder);

        JsonNode created = mapper.readTree(response.body());
        return created.get("id").asText();
    }

    private void updatePage(String id, String title, int nextVersion, String xhtml) throws IOException, InterruptedException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", id);
        payload.put("status", "current");
        payload.put("title", title);
        payload.put("body", Map.of("representation", "storage", "value", xhtml));
        payload.put("version", Map.of("number", nextVersion, "message", "Automated deployment"));

        String json = mapper.writeValueAsString(payload);
        HttpRequest.Builder builder = newPutBuilder(baseUrl + "/api/v2/pages/" + id, json);
        execute(builder);
    }

    private HttpRequest.Builder newGetBuilder(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET();
        authStrategy.authenticate(builder);
        return builder;
    }

    private HttpRequest.Builder newPostBuilder(String url, String json) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        authStrategy.authenticate(builder);
        return builder;
    }

    private HttpRequest.Builder newPutBuilder(String url, String json) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json));
        authStrategy.authenticate(builder);
        return builder;
    }

    private HttpResponse<String> execute(HttpRequest.Builder builder) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new RuntimeException("Confluence API Error: HTTP " + response.statusCode() + " -> " + response.body());
        }
        return response;
    }
}