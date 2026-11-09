package org.example.studyapp.app;

import java.nio.file.Path;
import org.example.studyapp.data_access.Config;
import org.example.studyapp.data_access.NotesFileReader;
import org.example.studyapp.data_access.GeminiGenerator;
import org.example.studyapp.data_access.LocalStore;
import org.example.studyapp.interface_adapter.generate.GenerateController;
import org.example.studyapp.interface_adapter.generate.GeneratePresenter;
import org.example.studyapp.interface_adapter.storage.StorageController;
import org.example.studyapp.use_case.generate.GenerateInteractor;
import org.example.studyapp.use_case.storage.StorageInteractor;
import org.example.studyapp.view.StudyWindow;

/** Composition root: concrete infrastructure is selected only here. */
public final class AppBuilder {
    public StudyWindow build() throws Exception {
        Config config = new Config(Path.of(".env"));
        LocalStore localStore = new LocalStore(Path.of(config.get("STUDY_APP_DATA_FILE",
                Path.of(System.getProperty("user.home"), ".study-app", "study-sets.json").toString())));
        StorageController local = new StorageController(new StorageInteractor(localStore));
        GenerateController generator = new GenerateController(new GenerateInteractor(new GeminiGenerator(config), new GeneratePresenter()));
        return new StudyWindow(local, generator, new NotesFileReader(), local.load());
    }
}
