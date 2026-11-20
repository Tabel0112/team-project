package data_access;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Environment takes priority over an ignored project-root .env file. Never logs values. */
public final class SupabaseConfig {
  final String url;
  final String publicKey;

  public SupabaseConfig(String url, String publicKey) {
    this.url = url == null ? "" : url.replaceAll("/+$", "");
    this.publicKey = publicKey == null ? "" : publicKey;
  }

  public static SupabaseConfig load() {
    Map<String, String> values = new HashMap<>();
    Path file = Path.of(".env");
    if (Files.isRegularFile(file)) {
      try {
        List<String> lines = Files.readAllLines(file);
        for (String line : lines) {
          String text = line.trim();
          if (text.startsWith("export ")) { text = text.substring(7).trim(); }
          int equals = text.indexOf('=');
          if (equals > 0 && !text.startsWith("#")) {
            String value = text.substring(equals + 1).trim();
            if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
              value = value.substring(1, value.length() - 1);
            }
            values.put(text.substring(0, equals).trim(), value);
          }
        }
      } catch (IOException ignored) {
        // Missing/unreadable configuration becomes a sanitized request failure.
      }
    }
    for (String name : new String[]{"SUPABASE_URL", "SUPABASE_ANON_KEY"}) {
      String value = System.getenv(name);
      if (value != null) { values.put(name, value); }
    }
    return new SupabaseConfig(values.get("SUPABASE_URL"), values.get("SUPABASE_ANON_KEY"));
  }

  boolean valid() {
    try {
      URI uri = URI.create(url);
      boolean transport = "https".equals(uri.getScheme()) || ("http".equals(uri.getScheme())
          && ("localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost())));
      return transport && uri.getHost() != null && uri.getUserInfo() == null
          && uri.getQuery() == null && uri.getFragment() == null && !publicKey.isBlank()
          && !publicKey.startsWith("sb_secret_") && !isServiceRoleJwt();
    } catch (IllegalArgumentException exception) { return false; }
  }

  private boolean isServiceRoleJwt() {
    try {
      String[] segments = publicKey.split("\\.");
      if (segments.length != 3) { return false; }
      String payload = new String(java.util.Base64.getUrlDecoder().decode(segments[1]),
          java.nio.charset.StandardCharsets.UTF_8);
      return "service_role".equals(com.google.gson.JsonParser.parseString(payload)
          .getAsJsonObject().get("role").getAsString());
    } catch (RuntimeException ignored) { return false; }
  }
}
