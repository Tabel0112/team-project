package data_access;

import com.google.gson.Gson;
import entity.Flashcard;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class GeminiQuestionsTest {
    private String response(List<?> pairs) {
        Gson gson = new Gson();
        Map<String, String> part = Map.of("text", gson.toJson(pairs));
        return gson.toJson(Map.of("candidates", List.of(Map.of("content", Map.of("parts", List.of(part))))));
    }

    @Test
    public void createsOneFlashcardPerPairInOrder() throws IOException {
        String body = response(List.of(Map.of("question", "First?", "answer", "One"),
                Map.of("question", "Second?", "answer", "Two")));
        List<Flashcard> cards = GeminiQuestions.parseFlashcards(body);
        assertEquals(2, cards.size());
        assertEquals("First?", cards.get(0).getQuestion());
        assertEquals("One", cards.get(0).getAnswer());
        assertEquals("Second?", cards.get(1).getQuestion());
        assertEquals("Two", cards.get(1).getAnswer());
    }

    @Test(expected = IOException.class)
    public void rejectsAnIncompletePair() throws IOException {
        GeminiQuestions.parseFlashcards(response(List.of(Map.of("question", "Missing answer?"))));
    }

    @Test(expected = IOException.class)
    public void handlesBlockedResponse() throws IOException {
        GeminiQuestions.parseFlashcards("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}");
    }
}
