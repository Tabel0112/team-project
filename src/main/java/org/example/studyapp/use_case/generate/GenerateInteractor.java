package org.example.studyapp.use_case.generate;

import org.example.studyapp.entity.StudySet;

public final class GenerateInteractor implements GenerateInputBoundary {
    private final GenerateDataAccessInterface generator;

    private final GenerateOutputBoundary presenter;

    public GenerateInteractor(GenerateDataAccessInterface generator, GenerateOutputBoundary presenter) {
        this.generator = generator;
        this.presenter = presenter;
    }

    @Override public GenerateOutputData execute(GenerateInputData input) throws Exception {
        if (input.title == null || input.title.isBlank() || input.notes == null || input.notes.isBlank())
            throw new IllegalArgumentException("Enter a title and notes first.");
        if (input.notes.length() > 100000)
            throw new IllegalArgumentException("Use at most 100,000 characters of notes.");
        if (input.difficulty == null || !java.util.List.of("Easy", "Medium", "Hard").contains(input.difficulty))
            throw new IllegalArgumentException("Choose Easy, Medium, or Hard difficulty.");
        if (input.count < 1 || input.count > 30)
            throw new IllegalArgumentException("Choose 1–30 questions.");
        StudySet result = generator.generate(input.title, input.notes, input.difficulty, input.count);
        result.validate();
        if (result.cards.isEmpty() || result.cards.size() > 30)
            throw new IllegalStateException("AI response was incomplete or invalid. Try fewer questions or different notes.");
        return presenter.prepareSuccessView(result);
    }
}
