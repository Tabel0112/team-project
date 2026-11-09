package org.example.studyapp.use_case.generate;

import org.example.studyapp.entity.StudySet;

public interface GenerateDataAccessInterface {
    StudySet generate(String title, String notes, String difficulty, int count) throws Exception;
}
