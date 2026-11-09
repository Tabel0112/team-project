package org.example.studyapp.data_access;

import org.example.studyapp.use_case.storage.StudySetDataAccessInterface;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.example.studyapp.entity.StudySet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/** Saves complete snapshots atomically, so interrupted writes preserve the old file. */
public final class LocalStore implements StudySetDataAccessInterface {
    private final Path file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public LocalStore(Path file) { this.file = file.toAbsolutePath(); }

    public List<StudySet> load() throws IOException {
        if (!Files.exists(file)) return new ArrayList<>();
        try {
            List<StudySet> sets = gson.fromJson(Files.readString(file),
                    new TypeToken<List<StudySet>>() { }.getType());
            if (sets == null) throw new IllegalArgumentException("Empty data file");
            for (StudySet set : sets) set.validate();
            return sets;
        } catch (RuntimeException e) {
            throw new IOException("Cannot read study sets. Your file was preserved at " + file, e);
        }
    }

    public void save(List<StudySet> sets) throws IOException {
        for (StudySet set : sets) set.validate();
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "study-", ".tmp");
        try {
            Files.writeString(temporary, gson.toJson(sets), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporary); }
    }
}
