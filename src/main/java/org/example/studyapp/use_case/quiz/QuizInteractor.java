package org.example.studyapp.use_case.quiz;

import java.util.List;
import java.util.stream.Collectors;
import org.example.studyapp.entity.Card;
import org.example.studyapp.entity.QuizSession;
import org.example.studyapp.entity.StudySet;

public final class QuizInteractor implements QuizInputBoundary {
    @Override public QuizSession start(StudySet studySet, boolean missedOnly) {
        studySet.validate();
        List<Card> pool = studySet.cards.stream().filter(card -> !missedOnly || card.missed)
                .collect(Collectors.toList());
        if (pool.isEmpty()) throw new IllegalArgumentException(missedOnly
                ? "No missed questions. Take a quiz first, or keep practicing your flashcards."
                : "Add a card or generate materials first.");
        return new QuizSession(pool);
    }
}
