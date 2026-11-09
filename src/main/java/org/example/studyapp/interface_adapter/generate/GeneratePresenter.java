package org.example.studyapp.interface_adapter.generate;

import org.example.studyapp.entity.StudySet;
import org.example.studyapp.use_case.generate.GenerateOutputBoundary;
import org.example.studyapp.use_case.generate.GenerateOutputData;

public final class GeneratePresenter implements GenerateOutputBoundary {
    @Override public GenerateOutputData prepareSuccessView(StudySet studySet) {
        return new GenerateOutputData(studySet, "Generated " + studySet.cards.size()
                + " cards in a new set. Review the answers, then save.");
    }
}
