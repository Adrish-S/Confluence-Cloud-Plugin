package io.github.adrish_s.confluence.auth;

import java.net.http.HttpRequest;

public class BearerTokenAuth implements AuthenticationStrategy {
    private final String bearerToken;

    public BearerTokenAuth(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    @Override
    public void authenticate(HttpRequest.Builder requestBuilder) {
        requestBuilder.header("Authorization", "Bearer " + bearerToken);
    }
}