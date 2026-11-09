package org.example.studyapp.interface_adapter.generate;

import org.example.studyapp.use_case.generate.GenerateOutputData;
import org.example.studyapp.use_case.generate.GenerateInputBoundary;
import org.example.studyapp.use_case.generate.GenerateInputData;

public final class GenerateController {
    private final GenerateInputBoundary interactor;

    public GenerateController(GenerateInputBoundary interactor) { this.interactor = interactor; }

    public GenerateOutputData generate(String title, String notes, String difficulty, int count) throws Exception {
        return interactor.execute(new GenerateInputData(title, notes, difficulty, count));
    }
}
