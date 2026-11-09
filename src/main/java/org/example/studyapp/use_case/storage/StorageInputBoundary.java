package org.example.studyapp.use_case.storage;

import java.util.List;
import org.example.studyapp.entity.StudySet;

public interface StorageInputBoundary {
    List<StudySet> load() throws Exception;
    void save(List<StudySet> sets) throws Exception;
}
