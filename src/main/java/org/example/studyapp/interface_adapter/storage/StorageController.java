package org.example.studyapp.interface_adapter.storage;

import java.util.List;
import org.example.studyapp.entity.StudySet;
import org.example.studyapp.use_case.storage.StorageInputBoundary;

public final class StorageController {
    private final StorageInputBoundary interactor;

    public StorageController(StorageInputBoundary interactor) { this.interactor = interactor; }

    public List<StudySet> load() throws Exception { return interactor.load(); }
    public void save(List<StudySet> sets) throws Exception { interactor.save(sets); }
}
