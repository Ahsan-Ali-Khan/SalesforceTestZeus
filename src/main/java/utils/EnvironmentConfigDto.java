package utils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EnvironmentConfigDto {
    private static final String CONFIG_PATH = "src/main/resources/environmentConfig.json";
    private static JsonNode root;

    static {
        try {
            ObjectMapper mapper = new ObjectMapper();
            root = mapper.readTree(new File(CONFIG_PATH)).get("environments");
        } catch (IOException e) {
            throw new RuntimeException("❌ Failed to load environmentConfig.json: " + e.getMessage(), e);
        }
    }

    public static JsonNode getEnvironment(String envName) {
        JsonNode env = root.get(envName);
        if (env == null) {
            throw new IllegalArgumentException("❌ Environment not found: " + envName);
        }
        return env;
    }

    public static String getGrantType(String envName) {
        return getEnvironment(envName).get("grantType").asText();
    }

    public static String getClientId(String envName) {
        return getEnvironment(envName).get("clientId").asText();
    }

    public static String getClientSecret(String envName) {
        return getEnvironment(envName).get("clientSecret").asText();
    }

    public static String getApiUrl(String envName) {
        return getEnvironment(envName).get("apiUrl").asText();
    }

    public static String getAppUrl(String envName) {
        return getEnvironment(envName).get("appUrl").asText();
    }

    public static Map<String, String> getRoleCredentials(String envName, String roleName) {
        JsonNode roles = getEnvironment(envName).get("roles");
        for (JsonNode role : roles) {
            if (role.get("role").asText().equalsIgnoreCase(roleName)) {
                Map<String, String> creds = new HashMap<>();
                creds.put("username", role.get("username").asText());
                creds.put("password", role.get("password").asText());
                return creds;
            }
        }
        throw new IllegalArgumentException("❌ Role not found in " + envName + ": " + roleName);
    }
}
