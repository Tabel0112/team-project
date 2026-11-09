package org.example.studyapp.use_case.storage;

import java.util.List;
import org.example.studyapp.entity.StudySet;

public final class StorageInteractor implements StorageInputBoundary {
    private final StudySetDataAccessInterface store;

    public StorageInteractor(StudySetDataAccessInterface store) { this.store = store; }

    @Override public List<StudySet> load() throws Exception {
        List<StudySet> sets = store.load();
        for (StudySet set : sets) set.validate();
        return sets;
    }

    @Override public void save(List<StudySet> sets) throws Exception {
        for (StudySet set : sets) set.validate();
        store.save(sets);
    }
}
