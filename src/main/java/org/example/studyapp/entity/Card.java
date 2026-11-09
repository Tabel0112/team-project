package org.example.studyapp.entity;

/** One editable question/answer pair and its retry status. */
public final class Card {
    public String question;
    public String answer;
    public boolean missed;

    public Card(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    @Override public String toString() { return question; }
}
