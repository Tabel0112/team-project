package org.example.studyapp.use_case.generate;

import org.example.studyapp.entity.StudySet;

/** Synchronous output port; presentation stays outside the application rules. */
public interface GenerateOutputBoundary {
    GenerateOutputData prepareSuccessView(StudySet studySet);
}
