package org.example.studyapp.interface_adapter.quiz;

import org.example.studyapp.entity.QuizSession;
import org.example.studyapp.entity.StudySet;
import org.example.studyapp.use_case.quiz.QuizInputBoundary;

public final class QuizController {
    private final QuizInputBoundary interactor;

    public QuizController(QuizInputBoundary interactor) { this.interactor = interactor; }

    public QuizSession start(StudySet studySet, boolean missedOnly) {
        return interactor.start(studySet, missedOnly);
    }
}
