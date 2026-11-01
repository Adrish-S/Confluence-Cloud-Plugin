package io.github.adrish_s.confluence.client;

public interface ConfluenceGateway {
    String upsertPage(ConfluencePageRequest request);
}