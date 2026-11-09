package org.example.studyapp.data_access;

import com.google.gson.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;

public final class ApiClient {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    public JsonElement request(String method, String url, Map<String, String> headers, JsonElement body)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(90))
                .header("Content-Type", "application/json");
        headers.forEach(builder::header);
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body.toString()));
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            // Do not expose response bodies: upstream errors can contain credentials or notes.
            throw new IOException("Service returned HTTP " + response.statusCode()
                    + ". Check credentials, account access, quota, and service setup.");
        }
        if (response.body().isBlank()) return JsonNull.INSTANCE;
        try { return JsonParser.parseString(response.body()); }
        catch (JsonParseException e) { throw new IOException("Service returned invalid JSON.", e); }
    }
}
