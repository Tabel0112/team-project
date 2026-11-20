package data_access;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.*;
import use_case.cloud.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

/** Real Java HTTP against loopback fixtures only. Never calls a live Supabase project. */
public class SupabaseGatewayTest {
  private HttpServer server;
  private SupabaseGateway gateway;
  private final String user = UUID.randomUUID().toString();
  private final String setId = UUID.randomUUID().toString();
  private final Queue<Reply> replies = new ConcurrentLinkedQueue<>();
  private final List<Request> calls = Collections.synchronizedList(new ArrayList<>());
  @Before public void setup() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      calls.add(new Request(exchange.getRequestURI().toString(),
          exchange.getRequestHeaders().getFirst("apikey"), exchange.getRequestHeaders().getFirst("Authorization"),
          new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
      Reply reply = replies.poll();
      if (reply == null) { reply = new Reply(500, "{\"error\":\"Unexpected fixture call\"}"); }
      byte[] bytes = reply.body.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(reply.status, bytes.length);
      exchange.getResponseBody().write(bytes); exchange.close();
    });
    server.start();
    gateway = new SupabaseGateway(new SupabaseConfig("http://127.0.0.1:" + server.getAddress().getPort(), "sb_publishable_fixture"));
  }
  @After public void cleanup() { if (server != null) { server.stop(0); } }
  private void reply(int status, String body) { replies.add(new Reply(status, body)); }
  private String session(String uid, String access, int expires) {
    return "{\"user\":{\"id\":\"" + uid + "\",\"email\":\"student@example.test\"},"
        + "\"access_token\":\"" + access + "\",\"refresh_token\":\"fixture-refresh\",\"expires_in\":" + expires + "}";
  }
  private void login() throws Exception {
    reply(200, session(user, "fixture-access", 3600));
    assertEquals(user, gateway.login("student@example.test", "fixture-password".toCharArray(), null).getId());
  }
  @Test public void publicKeyNeverSentAsBearerAndDataUsesAuthenticatedJwt() throws Exception {
    login(); reply(200, "[{\"id\":\"" + setId + "\",\"name\":\"Course\"}]");
    assertEquals("Course", gateway.list(user).get(0).name);
    assertEquals("sb_publishable_fixture", calls.get(0).key); assertNull(calls.get(0).authorization);
    assertEquals("Bearer fixture-access", calls.get(1).authorization);
    assertEquals("/auth/v1/token?grant_type=password", calls.get(0).path);
    assertTrue(calls.get(1).path.startsWith("/rest/v1/uc5_study_sets?"));
  }
  @Test public void expiredSessionRefreshesBeforeDataAndRetriesUnauthorizedOnce() throws Exception {
    reply(200, session(user, "old", 0)); gateway.login("student@example.test", "fixture".toCharArray(), null);
    reply(200, session(user, "new", 3600)); reply(401, "{}");
    reply(200, session(user, "newer", 3600)); reply(200, "[]");
    assertTrue(gateway.list(user).isEmpty());
    assertEquals("/auth/v1/token?grant_type=refresh_token", calls.get(1).path);
    assertNull(calls.get(1).authorization);
    assertEquals("Bearer new", calls.get(2).authorization);
    assertEquals("Bearer newer", calls.get(4).authorization);
  }
  @Test public void differentAccountCannotReplaceDraftSession() throws Exception {
    login(); reply(200, session(UUID.randomUUID().toString(), "other", 3600));
    CloudException error = assertThrows(CloudException.class,
        () -> gateway.login("other@example.test", "fixture".toCharArray(), user));
    assertTrue(error.getMessage().contains("same account"));
    reply(200, "[]"); gateway.list(user);
    assertEquals("Bearer fixture-access", calls.get(2).authorization);
  }
  @Test public void rpcDeltaCarriesStableOperationAndIdenticalRetryPayload() throws Exception {
    login();
    SetSnapshot snapshot = new SetSnapshot(setId, "Course", 0,
        List.of(new SetSnapshot.Card(UUID.randomUUID().toString(), "Question", "Answer", 0)));
    SaveRequest save = new SaveRequest(user, snapshot, null);
    reply(503, "{\"message\":\"fixture-token-must-not-leak\"}");
    CloudException failure = assertThrows(CloudException.class, () -> gateway.save(save));
    assertFalse(failure.getMessage().contains("fixture-token"));
    reply(200, "1"); assertEquals(1, gateway.save(save));
    assertEquals(calls.get(1).body, calls.get(2).body);
    JsonObject body = JsonParser.parseString(calls.get(2).body).getAsJsonObject();
    assertEquals(save.operationId, body.get("p_operation_id").getAsString());
    assertEquals(snapshot.cards.get(0).id, body.getAsJsonArray("p_upserts").get(0).getAsJsonObject().get("id").getAsString());
    assertEquals("/rest/v1/rpc/uc5_save_set", calls.get(2).path);
  }
  @Test public void loadReconstructsStableCardOrderAndHandlesMissingSet() throws Exception {
    login(); String card = UUID.randomUUID().toString();
    reply(200, "{\"id\":\"" + setId + "\",\"name\":\"Course\",\"revision\":2,\"cards\":[{\"id\":\""
        + card + "\",\"question\":\"Q\",\"answer\":\"A\",\"position\":0}]}");
    SetSnapshot result = gateway.load(user, setId);
    assertEquals(2, result.revision); assertEquals(card, result.cards.get(0).id);
    assertEquals("A", result.toEntity().getCardsList().getCards().get(0).getAnswer());
    reply(200, "null"); assertThrows(CloudException.class, () -> gateway.load(user, setId));
  }
  @Test public void malformedJsonAndValidJsonWithWrongShapesAreSanitized() throws Exception {
    for (String body : List.of("[]", "null", "{}", "{\"user\":{\"id\":\"bad\"}}", "broken")) {
      reply(200, body);
      CloudException failure = assertThrows(CloudException.class,
          () -> gateway.login("student@example.test", "fixture".toCharArray(), null));
      assertTrue(failure.getMessage().contains("unexpected response"));
    }
    login();
    for (String body : List.of("{}", "null", "[{}]", "[{\"id\":\"bad\",\"name\":\"sensitive\"}]")) {
      reply(200, body);
      CloudException failure = assertThrows(CloudException.class, () -> gateway.list(user));
      assertTrue(failure.getMessage().contains("unexpected response")); assertFalse(failure.getMessage().contains("sensitive"));
    }
  }
  @Test public void conflictAndExpiredRefreshDoNotExposeResponseBody() throws Exception {
    login(); reply(409, "{\"token\":\"sensitive\"}");
    CloudException error = assertThrows(CloudException.class, () -> gateway.save(
        new SaveRequest(user, new SetSnapshot(setId, "Course", 0, List.of()), null)));
    assertTrue(error.getMessage().contains("cloud copy changed")); assertFalse(error.getMessage().contains("sensitive"));
    reply(401, "{}"); reply(401, "{\"message\":\"sensitive\"}");
    CloudException expired = assertThrows(CloudException.class, () -> gateway.list(user));
    assertTrue(expired.getMessage().contains("same account"));
  }
  @Test public void refusesSecretKeysBeforeSendingAnyRequest() {
    SupabaseGateway unsafe = new SupabaseGateway(new SupabaseConfig("http://127.0.0.1:" + server.getAddress().getPort(), "sb_secret_fixture"));
    assertThrows(CloudException.class, () -> unsafe.login("student@example.test", "fixture".toCharArray(), null));
    assertTrue(calls.isEmpty());
  }
  private static final class Reply {
    final int status; final String body;
    Reply(int status, String body) { this.status = status; this.body = body; }
  }
  private static final class Request {
    final String path, key, authorization, body;
    Request(String path, String key, String authorization, String body) {
      this.path = path; this.key = key; this.authorization = authorization; this.body = body;
    }
  }
}
