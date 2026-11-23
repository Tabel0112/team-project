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
  @Test public void loginAndSignupUseAuthEndpointAndPublicKeyWithoutBearer() throws Exception {
    login();
    reply(200, "{}"); gateway.signup("new@example.test", "fixture".toCharArray());
    assertEquals("sb_publishable_fixture", calls.get(0).key);
    assertNull(calls.get(0).authorization); assertNull(calls.get(1).authorization);
    assertEquals("/auth/v1/token?grant_type=password", calls.get(0).path);
    assertEquals("/auth/v1/signup", calls.get(1).path);
    assertEquals("student@example.test", JsonParser.parseString(calls.get(0).body)
        .getAsJsonObject().get("email").getAsString());
  }
  @Test public void differentAccountCannotReplaceSessionAndLogoutClearsIt() throws Exception {
    login(); reply(200, session(UUID.randomUUID().toString(), "other", 3600));
    CloudException error = assertThrows(CloudException.class,
        () -> gateway.login("other@example.test", "fixture".toCharArray(), user));
    assertTrue(error.getMessage().contains("same account"));
    java.lang.reflect.Field field = SupabaseGateway.class.getDeclaredField("session");
    field.setAccessible(true); Object retained = field.get(gateway);
    assertNotNull(retained);
    gateway.forgetSession(); assertNull(field.get(gateway));
  }
  @Test public void malformedOrRejectedResponsesNeverExposeResponseBody() throws Exception {
    for (String body : List.of("[]", "null", "{}", "{\"user\":{\"id\":\"bad\"}}", "broken")) {
      reply(200, body);
      CloudException error = assertThrows(CloudException.class,
          () -> gateway.login("student@example.test", "fixture".toCharArray(), null));
      assertTrue(error.getMessage().contains("unexpected response"));
    }
    reply(400, "{\"message\":\"sensitive-token\"}");
    CloudException error = assertThrows(CloudException.class,
        () -> gateway.login("student@example.test", "fixture".toCharArray(), null));
    assertTrue(error.getMessage().contains("Authentication failed"));
    assertFalse(error.getMessage().contains("sensitive"));
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
