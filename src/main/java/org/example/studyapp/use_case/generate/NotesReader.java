package org.example.studyapp.use_case.generate;

import java.nio.file.Path;

public interface NotesReader {
    String read(Path path) throws Exception;
}
