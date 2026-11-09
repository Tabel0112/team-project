package org.example.studyapp.entity;

import java.util.ArrayList;
import java.util.List;

/** Short-answer practice uses explicit self-assessment against the reference answer. */
public final class QuizSession {
    private final List<Card> cards;
    private int position;
    private int correct;

    public QuizSession(List<Card> cards) {
        if (cards.isEmpty()) throw new IllegalArgumentException("Add cards before starting a quiz.");
        this.cards = new ArrayList<>(cards);
    }
    public Card current() { return cards.get(position); }
    public int position() { return position; }
    public int total() { return cards.size(); }
    public int correct() { return correct; }
    public boolean finished() { return position == cards.size(); }
    public void answer(boolean knewAnswer) {
        if (finished()) throw new IllegalStateException("Quiz is finished.");
        current().missed = !knewAnswer;
        if (knewAnswer) correct++;
        position++;
    }
    public void expire() { while (!finished()) answer(false); }
}
