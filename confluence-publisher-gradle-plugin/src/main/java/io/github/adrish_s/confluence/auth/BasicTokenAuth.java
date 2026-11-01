package io.github.adrish_s.confluence.auth;

import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class BasicTokenAuth implements AuthenticationStrategy {
    private final String email;
    private final String apiToken;

    public BasicTokenAuth(String email, String apiToken) {
        this.email = email;
        this.apiToken = apiToken;
    }

    @Override
    public void authenticate(HttpRequest.Builder requestBuilder) {
        String token = email + ":" + apiToken;
        String encoded = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
        requestBuilder.header("Authorization", "Basic " + encoded);
    }
}