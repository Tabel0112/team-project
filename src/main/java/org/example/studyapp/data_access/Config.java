package org.example.studyapp.data_access;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Environment variables override the project's optional .env file. */
public final class Config {
    private final Map<String, String> values = new HashMap<>();

    public Config(Path file) throws IOException {
        if (Files.exists(file)) {
            for (String line : Files.readAllLines(file)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("export ")) line = line.substring(7).trim();
                int split = line.indexOf('=');
                if (split < 1) throw new IOException("Invalid .env entry; expected KEY=value.");
                String value = line.substring(split + 1).trim();
                if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }
                values.put(line.substring(0, split).trim(), value);
            }
        }
        values.putAll(System.getenv());
    }

    public String get(String key, String fallback) {
        String value = values.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    public String require(String key) {
        String value = get(key, "");
        if (value.isBlank()) throw new IllegalStateException("Fill in " + key + " in .env and restart the app.");
        return value;
    }
}
