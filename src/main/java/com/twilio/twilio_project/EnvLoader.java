package com.twilio.twilio_project;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvBuilder;
import java.nio.file.Files;
import java.nio.file.Paths;

public class EnvLoader {
    private static final Dotenv dotenv;

    static {
        DotenvBuilder builder = Dotenv.configure().ignoreIfMissing();
        String[] candidates = {
            System.getProperty("user.dir"),
            Paths.get("").toAbsolutePath().toString(),
            System.getProperty("user.dir") + "/../Twilio-SMS-Client",
        };
        for (String dir : candidates) {
            if (dir != null && Files.exists(Paths.get(dir, ".env"))) {
                builder.directory(dir);
                break;
            }
        }
        dotenv = builder.load();
    }

    public static String get(String key) {
        String profile = getProfile();
        String value = resolveForProfile(key, profile);
        if (value == null) {
            value = dotenv.get(key);
        }
        if (value == null) {
            value = System.getenv(key);
        }
        return value;
    }

    private static String getProfile() {
        String p = System.getProperty("app.profile");
        if (p == null) p = dotenv.get("APP_PROFILE");
        if (p == null) p = System.getenv("APP_PROFILE");
        return "docker".equalsIgnoreCase(p) ? "docker" : "local";
    }

    private static String resolveForProfile(String key, String profile) {
        String prefix = "docker".equals(profile) ? "DOCKER_" : "LOCAL_";
        String profileKey = prefix + key;
        String value = dotenv.get(profileKey);
        if (value != null) return value;
        // Fallback to non-prefixed key
        return dotenv.get(key);
    }

    public static String getProfileName() {
        return getProfile();
    }
}