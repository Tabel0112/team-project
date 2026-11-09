package org.example.studyapp.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One subject's notes and editable question/answer cards. */
public final class StudySet {
    public String id = UUID.randomUUID().toString();
    public String title = "Untitled set";
    public String notes = "";
    public String difficulty = "Medium";
    public List<Card> cards = new ArrayList<>();

    public void validate() {
        if (id == null || title == null || title.isBlank() || notes == null || cards == null
                || !("Easy".equals(difficulty) || "Medium".equals(difficulty) || "Hard".equals(difficulty))) {
            throw new IllegalArgumentException("Study set has invalid or missing fields.");
        }
        UUID.fromString(id);
        for (Card card : cards) {
            if (card == null || card.question == null || card.question.isBlank()
                    || card.answer == null || card.answer.isBlank()) {
                throw new IllegalArgumentException("Every card needs a question and an answer.");
            }
        }
    }

    @Override public String toString() { return title + " (" + cards.size() + ")"; }
}
