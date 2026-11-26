package data_access;

import com.google.gson.*;
import entity.User;
import use_case.cloud.*;
import use_case.login.LoginDataAccessInterface;
import use_case.load_study_sets.LoadStudySetsDataAccessInterface;
import use_case.save_study_set.SaveAllStudySetsDataAccessInterface;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Java 11 HTTP adapter. Public API key identifies the app; the access JWT identifies the user. */
public final class SupabaseGateway implements LoginDataAccessInterface,
    LoadStudySetsDataAccessInterface, SaveAllStudySetsDataAccessInterface {
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

  @Override public synchronized List<SetSummary> list(String userId) throws CloudException {
    try {
      List<SetSummary> result = new ArrayList<>();
      int offset = 0;
      while (true) {
        JsonArray rows = authorized(userId, "GET", "/rest/v1/uc5_study_sets?select=id,name"
            + "&order=id&limit=500&offset=" + offset, null).getAsJsonArray();
        for (JsonElement row : rows) {
          JsonObject set = row.getAsJsonObject();
          result.add(new SetSummary(uuid(set, "id"), string(set, "name")));
        }
        if (rows.size() < 500) { break; }
        offset += rows.size();
      }
      return List.copyOf(result);
    } catch (RuntimeException exception) { throw malformed(); }
  }
  @Override public synchronized SetSnapshot load(String userId, String setId) throws CloudException {
    JsonObject body = new JsonObject(); body.addProperty("p_set_id", setId);
    JsonElement element = authorized(userId, "POST", "/rest/v1/rpc/uc5_load_set", body);
    if (element.isJsonNull()) { throw new CloudException("This study set is unavailable. Your local drafts were kept."); }
    try {
      JsonObject set = element.getAsJsonObject();
      List<SetSnapshot.Card> cards = new ArrayList<>();
      for (JsonElement row : set.getAsJsonArray("cards")) {
        JsonObject card = row.getAsJsonObject();
        cards.add(new SetSnapshot.Card(uuid(card, "id"), string(card, "question"),
            string(card, "answer"), card.get("position").getAsInt()));
      }
      if (!setId.equals(uuid(set, "id"))) { throw new IllegalArgumentException(); }
      return new SetSnapshot(setId, string(set, "name"), set.get("revision").getAsLong(), cards);
    } catch (RuntimeException exception) { throw malformed(); }
  }
  @Override public synchronized long save(SaveRequest request) throws CloudException {
    JsonObject body = new JsonObject();
    body.addProperty("p_operation_id", request.operationId);
    body.addProperty("p_set_id", request.snapshot.id);
    body.addProperty("p_name", request.snapshot.name);
    body.addProperty("p_expected_revision", request.expectedRevision);
    body.add("p_upserts", gson.toJsonTree(request.upserts));
    body.add("p_deleted_ids", gson.toJsonTree(request.deletedIds));
    JsonElement response = authorized(request.userId, "POST", "/rest/v1/rpc/uc5_save_set", body);
    try {
      long revision = response.getAsLong();
      if (revision != request.expectedRevision + 1) { throw new IllegalArgumentException(); }
      return revision;
    } catch (RuntimeException exception) { throw malformed(); }
  }
  @Override public synchronized void delete(String userId, String setId) throws CloudException {
    String id;
    try {
      id = UUID.fromString(setId).toString();
      if (!id.equalsIgnoreCase(setId)) { throw new IllegalArgumentException(); }
    } catch (RuntimeException exception) {
      throw new CloudException("The study set ID is invalid. Its deletion was kept for review.");
    }
    // PostgREST DELETE is idempotent for an already absent row. RLS verifies ownership.
    authorized(userId, "DELETE", "/rest/v1/uc5_study_sets?id=eq." + id, null);
  }

  private JsonElement authorized(String userId, String method, String path, JsonObject body)
      throws CloudException {
    requireUser(userId);
    if (session.expiresAt <= Instant.now().getEpochSecond() + 30) { refresh(); }
    try {
      return request(method, path, body, session.accessToken);
    } catch (Unauthorized exception) {
      refresh();
      try { return request(method, path, body, session.accessToken); }
      catch (Unauthorized expired) { throw new CloudException("Your session expired. Log in again with the same account; local changes were kept."); }
    }
  }
  private void requireUser(String userId) throws CloudException {
    if (session == null || !session.user.getId().equals(userId)) {
      throw new CloudException("Log in again to save or load. Your local changes were kept.");
    }
  }
  private void refresh() throws CloudException {
    JsonObject body = new JsonObject(); body.addProperty("refresh_token", session.refreshToken);
    Session previous = session;
    try {
      Session candidate = decodeSession(request("POST", "/auth/v1/token?grant_type=refresh_token", body, null)
          .getAsJsonObject());
      if (!candidate.user.getId().equals(previous.user.getId())) { throw malformed(); }
      session = candidate;
    } catch (Unauthorized exception) {
      throw new CloudException("Your session expired. Log in again with the same account; local changes were kept.");
    } catch (RuntimeException exception) { throw malformed(); }
  }
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
        if (status == 409 || isConflict(response.body())) {
          throw new CloudException("The cloud copy changed. Your local changes were kept. Save in one app at a time; reconcile this draft before reloading.");
        }
        if (path.startsWith("/auth/")) {
          throw new CloudException("Authentication failed. Check your credentials, email confirmation, and account settings, then try again.");
        }
        throw new CloudException("Cloud request failed. Check database setup and access, then retry; local changes were kept.");
      }
      return response.body().isBlank() ? JsonNull.INSTANCE : JsonParser.parseString(response.body());
    } catch (IOException exception) {
      throw new CloudException("Could not reach the cloud. Check your connection and retry; local changes were kept.");
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new CloudException("The request was interrupted. Retry when ready; local changes were kept.");
    } catch (RuntimeException exception) { throw malformed(); }
  }
  private boolean isConflict(String body) {
    try { return "40001".equals(JsonParser.parseString(body).getAsJsonObject().get("code").getAsString()); }
    catch (RuntimeException exception) { return false; }
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
