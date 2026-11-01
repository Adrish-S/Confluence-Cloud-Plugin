package io.github.adrish_s.confluence.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfluenceConfig {
    private String baseUrl;
    private String email;
    private String apiToken;
    private String bearerToken;
    private String spaceKey;
    private String rootParentId;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getApiToken() { return apiToken; }
    public void setApiToken(String apiToken) { this.apiToken = apiToken; }

    public String getBearerToken() { return bearerToken; }
    public void setBearerToken(String bearerToken) { this.bearerToken = bearerToken; }

    public String getSpaceKey() { return spaceKey; }
    public void setSpaceKey(String spaceKey) { this.spaceKey = spaceKey; }

    public String getRootParentId() { return rootParentId; }
    public void setRootParentId(String rootParentId) { this.rootParentId = rootParentId; }
}