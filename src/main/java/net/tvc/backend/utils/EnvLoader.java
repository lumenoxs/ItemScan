package net.tvc.backend.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.tvc.backend.BackendInstance;

/**
 * Minimal .env loader. Reads a file named `.env` in the plugin data directory.
 * Lines starting with `#` are ignored. Simple KEY=VALUE parsing.
 */
public final class EnvLoader {
    private static volatile Map<String, String> env = null;

    private EnvLoader() {
    }

    private static void load() {
        if (env != null)
            return;
        synchronized (EnvLoader.class) {
            if (env != null)
                return;
            Map<String, String> map = new HashMap<>();
            try {
                Path dataDir = Paths.get(System.getProperty("user.home"));
                BackendInstance.LOGGER.info("Looking for .env file in " + dataDir);
                if (dataDir != null) {
                    Path envFile = dataDir.resolve(".env");
                    if (Files.exists(envFile)) {
                        try (BufferedReader br = Files.newBufferedReader(envFile)) {
                            String line;
                            while ((line = br.readLine()) != null) {
                                line = line.trim();
                                if (line.isEmpty() || line.startsWith("#"))
                                    continue;
                                int idx = line.indexOf('=');
                                if (idx <= 0)
                                    continue;
                                String key = line.substring(0, idx).trim();
                                String val = line.substring(idx + 1).trim();
                                // strip optional surrounding quotes
                                if ((val.startsWith("\"") && val.endsWith("\""))
                                        || (val.startsWith("'") && val.endsWith("'"))) {
                                    val = val.substring(1, val.length() - 1);
                                }
                                map.put(key, val);
                            }
                        }
                    }
                }
            } catch (IOException | RuntimeException e) {
                // don't fail hard; leave map empty
                try {
                    BackendInstance.LOGGER.warn("Failed to read .env file: " + e.getMessage());
                } catch (Exception ignored) {
                }
            }
            env = Collections.unmodifiableMap(map);
        }
    }

    public static String get(String key) {
        if (env == null)
            load();
        return env.get(key);
    }
}
