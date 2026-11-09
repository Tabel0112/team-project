package org.example.studyapp.use_case.generate;

public final class GenerateInputData {
    public final String title;
    public final String notes;
    public final String difficulty;
    public final int count;

    public GenerateInputData(String title, String notes, String difficulty, int count) {
        this.title = title;
        this.notes = notes;
        this.difficulty = difficulty;
        this.count = count;
    }
}
