package io.github.adrish_s.confluence.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigLoader {
    private static final Pattern ENV_VAR_PATTERN = Pattern.compile("\\$\\{([A-Za-z0-9_]+)}");

    public static ConfluenceConfig load(File configFile) throws IOException {
        if (!configFile.exists() || !configFile.isFile()) {
            throw new IllegalArgumentException("Config file does not exist: " + configFile.getAbsolutePath());
        }

        String rawContent = Files.readString(configFile.toPath());
        String resolvedContent = interpolateEnvironmentVariables(rawContent);

        String fileName = configFile.getName().toLowerCase();
        ObjectMapper mapper;
        if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
            mapper = new ObjectMapper(new YAMLFactory());
        } else if (fileName.endsWith(".json")) {
            mapper = new ObjectMapper();
        } else {
            throw new IllegalArgumentException("Unsupported config extension. Use .json, .yaml, or .yml: " + fileName);
        }

        ConfluenceConfig config = mapper.readValue(resolvedContent, ConfluenceConfig.class);
        validate(config);
        return config;
    }

    private static String interpolateEnvironmentVariables(String input) {
        Matcher matcher = ENV_VAR_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String envName = matcher.group(1);
            String envValue = System.getenv(envName);
            if (envValue == null) {
                envValue = System.getProperty(envName, "");
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(envValue));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static void validate(ConfluenceConfig config) {
        if (config.getBaseUrl() == null || config.getBaseUrl().isBlank()) {
            throw new IllegalArgumentException("Config validation error: 'baseUrl' is required.");
        }
        if (config.getSpaceKey() == null || config.getSpaceKey().isBlank()) {
            throw new IllegalArgumentException("Config validation error: 'spaceKey' is required.");
        }
        boolean hasBasicAuth = (config.getEmail() != null && !config.getEmail().isBlank())
                && (config.getApiToken() != null && !config.getApiToken().isBlank());
        boolean hasBearerAuth = config.getBearerToken() != null && !config.getBearerToken().isBlank();

        if (!hasBasicAuth && !hasBearerAuth) {
            throw new IllegalArgumentException("Config validation error: Must provide either (email + apiToken) or bearerToken.");
        }
    }
}