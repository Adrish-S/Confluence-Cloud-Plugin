package io.github.adrish_s.confluence.auth;

import java.net.http.HttpRequest;

public interface AuthenticationStrategy {
    void authenticate(HttpRequest.Builder requestBuilder);
}