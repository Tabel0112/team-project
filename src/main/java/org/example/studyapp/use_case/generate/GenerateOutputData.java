package org.example.studyapp.use_case.generate;

import org.example.studyapp.entity.StudySet;

public final class GenerateOutputData {
    public final StudySet studySet;
    public final String message;

    public GenerateOutputData(StudySet studySet, String message) {
        this.studySet = studySet;
        this.message = message;
    }
}
