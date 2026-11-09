package org.example.studyapp.use_case.quiz;

import org.example.studyapp.entity.QuizSession;
import org.example.studyapp.entity.StudySet;

public interface QuizInputBoundary {
    QuizSession start(StudySet studySet, boolean missedOnly);
}
