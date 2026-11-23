package data_access;

import com.google.gson.*;
import entity.User;
import use_case.cloud.CloudException;
import use_case.login.LoginDataAccessInterface;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Java 11 HTTP adapter. Public API key identifies the app; the access JWT identifies the user. */
public final class SupabaseGateway implements LoginDataAccessInterface {
  private final SupabaseConfig config;
  private final HttpClient http;
  private final Gson gson = new Gson();
  // Tokens remain only in memory, never User, disk, response messages, or logs.
  private Session session;

  public SupabaseGateway(SupabaseConfig config) {
    this(config, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
  }
  public SupabaseGateway(SupabaseConfig config, HttpClient http) {
    this.config = config; this.http = http;
  }
  @Override public synchronized User login(String email, char[] password, String requiredUserId)
      throws CloudException {
    JsonObject body = new JsonObject();
    body.addProperty("email", email); body.addProperty("password", new String(password));
    Session candidate;
    try {
      candidate = decodeSession(request("POST", "/auth/v1/token?grant_type=password", body, null).getAsJsonObject());
    } catch (RuntimeException exception) { throw malformed(); }
    if (requiredUserId != null && !requiredUserId.equals(candidate.user.getId())) {
      throw new CloudException("Log in to the same account to keep your drafts, or log out and discard them first.");
    }
    session = candidate;
    return candidate.user;
  }
  @Override public synchronized void signup(String email, char[] password) throws CloudException {
    JsonObject body = new JsonObject();
    body.addProperty("email", email); body.addProperty("password", new String(password));
    request("POST", "/auth/v1/signup", body, null);
  }
  @Override public synchronized void forgetSession() { session = null; }

  private Session decodeSession(JsonObject json) throws CloudException {
    try {
      JsonObject identity = json.getAsJsonObject("user");
      User user = new User(uuid(identity, "id"), string(identity, "email"));
      String access = string(json, "access_token");
      String refresh = string(json, "refresh_token");
      if (access.isBlank() || refresh.isBlank()) { throw new IllegalArgumentException(); }
      return new Session(user, access, refresh, Instant.now().getEpochSecond() + json.get("expires_in").getAsLong());
    } catch (RuntimeException exception) { throw malformed(); }
  }
  private JsonElement request(String method, String path, JsonObject body, String token)
      throws CloudException {
    if (!config.valid()) { throw new CloudException("Configure SUPABASE_URL and a public SUPABASE_ANON_KEY in the environment or ignored .env file."); }
    try {
      HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(config.url + path))
          .timeout(Duration.ofSeconds(25)).header("apikey", config.publicKey)
          .header("Accept", "application/json");
      if (token != null) { builder.header("Authorization", "Bearer " + token); }
      if (body == null) { builder.method(method, HttpRequest.BodyPublishers.noBody()); }
      else { builder.header("Content-Type", "application/json")
          .method(method, HttpRequest.BodyPublishers.ofString(gson.toJson(body))); }
      HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      int status = response.statusCode();
      if (status == 401 || status == 403) { throw new Unauthorized(); }
      if (status < 200 || status >= 300) {
        if (path.startsWith("/auth/")) {
          throw new CloudException("Authentication failed. Check your credentials, email confirmation, and account settings, then try again.");
        }
        throw new CloudException("Authentication failed. Check your credentials and account settings, then retry.");
      }
      return response.body().isBlank() ? JsonNull.INSTANCE : JsonParser.parseString(response.body());
    } catch (IOException exception) {
      throw new CloudException("Could not reach the cloud. Check your connection and retry; local changes were kept.");
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new CloudException("The request was interrupted. Retry when ready; local changes were kept.");
    } catch (RuntimeException exception) { throw malformed(); }
  }
  private static String string(JsonObject json, String field) { return json.get(field).getAsString(); }
  private static String uuid(JsonObject json, String field) { return UUID.fromString(string(json, field)).toString(); }
  private static CloudException malformed() { return new CloudException("The cloud returned an unexpected response. Local changes were kept; check setup before retrying."); }
  private static final class Session {
    final User user;
    final String accessToken;
    final String refreshToken;
    final long expiresAt;
    Session(User user, String accessToken, String refreshToken, long expiresAt) {
      this.user = user; this.accessToken = accessToken; this.refreshToken = refreshToken; this.expiresAt = expiresAt;
    }
  }
  private static final class Unauthorized extends CloudException {
    Unauthorized() { super("Authentication failed or your session expired. Log in again; local changes were kept."); }
  }
}
