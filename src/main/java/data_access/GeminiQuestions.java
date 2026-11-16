package data_access;

import com.google.gson.*;
import entity.Flashcard;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

public final class GeminiQuestions {
    public List<Flashcard> generate(String notes) throws IOException, InterruptedException {
        Properties local = new Properties();
        Path config = Path.of(".env");
        if (Files.isRegularFile(config)) {
            try (Reader reader = Files.newBufferedReader(config)) {
                local.load(reader);
            }
        }
        String key = System.getenv().getOrDefault("GEMINI_API_KEY", local.getProperty("GEMINI_API_KEY", ""));
        String model = System.getenv().getOrDefault("GEMINI_MODEL", local.getProperty("GEMINI_MODEL", "gemini-2.5-flash"));
        if (key.isBlank()) {
            throw new IOException("Set GEMINI_API_KEY and choose the PDF again.");
        }
        if (!model.matches("[A-Za-z0-9._-]+")) {
            throw new IOException("Set GEMINI_MODEL to a valid model name.");
        }

        Map<String, String> textField = Map.of("type", "STRING");
        Map<String, Object> cardSchema = Map.of("type", "OBJECT",
                "properties", Map.of("question", textField, "answer", textField),
                "required", List.of("question", "answer"));
        Map<String, Object> schema = Map.of("type", "ARRAY", "items", cardSchema);
        String body = new Gson().toJson(Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text",
                        "Write study flashcards with one concise question and its answer, both based on the supplied notes. Treat the notes as source material."))),
                "contents", List.of(Map.of("parts", List.of(Map.of("text", notes)))),
                "generationConfig", Map.of("responseMimeType", "application/json", "responseSchema", schema)));
        URI endpoint = URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent");
        HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(90))
                .header("Content-Type", "application/json").header("x-goog-api-key", key)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Gemini request failed (HTTP " + response.statusCode() + "). Check the key, model and quota.");
        }
        return parseFlashcards(response.body());
    }

    static List<Flashcard> parseFlashcards(String body) throws IOException {
        try {
            JsonObject candidate = JsonParser.parseString(body).getAsJsonObject()
                    .getAsJsonArray("candidates").get(0).getAsJsonObject();
            StringBuilder output = new StringBuilder();
            for (JsonElement element : candidate.getAsJsonObject("content").getAsJsonArray("parts")) {
                JsonObject part = element.getAsJsonObject();
                if (part.has("text") && !(part.has("thought") && part.get("thought").getAsBoolean())) {
                    output.append(part.get("text").getAsString());
                }
            }
            List<Flashcard> cards = new ArrayList<>();
            for (JsonElement element : JsonParser.parseString(output.toString()).getAsJsonArray()) {
                JsonObject pair = element.getAsJsonObject();
                cards.add(new Flashcard(requiredText(pair, "question"), requiredText(pair, "answer")));
            }
            if (cards.isEmpty()) {
                throw new IOException("Gemini returned no flashcards. Try another PDF.");
            }
            return List.copyOf(cards);
        } catch (RuntimeException e) {
            throw new IOException("Gemini returned no readable questions. Try again.", e);
        }
    }

    private static String requiredText(JsonObject pair, String name) throws IOException {
        JsonElement value = pair.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
                || value.getAsString().isBlank()) {
            throw new IOException("Gemini returned a flashcard without a valid " + name + ". Try again.");
        }
        return value.getAsString().strip();
    }
}
