package io.github.adrish_s.confluence.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.adrish_s.confluence.auth.BasicTokenAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class HttpConfluenceGatewayTest {

    private HttpServer server;
    private HttpConfluenceGateway gateway;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(null);
        server.start();

        int port = server.getAddress().getPort();
        String mockBaseUrl = "http://localhost:" + port + "/wiki";
        gateway = new HttpConfluenceGateway(mockBaseUrl, new BasicTokenAuth("test@user.com", "token123"));
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldCreatePageWhenNotPresent() {
        AtomicReference<String> postBody = new AtomicReference<>();

        server.createContext("/wiki/api/v2/spaces", exchange ->
                sendResponse(exchange, 200, "{\"results\":[{\"id\":\"1001\",\"key\":\"DEV\"}]}"));

        server.createContext("/wiki/api/v2/pages", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendResponse(exchange, 200, "{\"results\":[]}");
                } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    postBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                    sendResponse(exchange, 201, "{\"id\":\"7777\",\"version\":{\"number\":1}}");
                }
            }
        });

        String pageId = gateway.upsertPage(new ConfluencePageRequest("Overview", "DEV", null, "<p>Doc</p>"));

        assertEquals("7777", pageId);
        assertNotNull(postBody.get());
        assertTrue(postBody.get().contains("\"spaceId\":\"1001\""));
        assertTrue(postBody.get().contains("\"title\":\"Overview\""));
        assertTrue(postBody.get().contains("\"representation\":\"storage\""));
    }

    @Test
    void shouldUpdatePageWhenPresentWithBumpingVersion() {
        AtomicReference<String> putBody = new AtomicReference<>();

        server.createContext("/wiki/api/v2/spaces", exchange ->
                sendResponse(exchange, 200, "{\"results\":[{\"id\":\"1001\",\"key\":\"DEV\"}]}"));

        server.createContext("/wiki/api/v2/pages", exchange ->
                sendResponse(exchange, 200, "{\"results\":[{\"id\":\"8888\",\"title\":\"Existing\",\"version\":{\"number\":2}}]}"));

        server.createContext("/wiki/api/v2/pages/8888", exchange -> {
            putBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendResponse(exchange, 200, "{\"id\":\"8888\",\"version\":{\"number\":3}}");
        });

        String pageId = gateway.upsertPage(new ConfluencePageRequest("Existing", "DEV", null, "<p>New</p>"));

        assertEquals("8888", pageId);
        assertNotNull(putBody.get());
        assertTrue(putBody.get().contains("\"number\":3"));
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
        byte[] bytes = responseText.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}