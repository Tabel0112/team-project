package org.example.studyapp.app;

import java.nio.file.Path;
import org.example.studyapp.data_access.Config;
import org.example.studyapp.data_access.CloudStore;
import org.example.studyapp.data_access.GeminiGenerator;
import org.example.studyapp.data_access.LocalStore;
import org.example.studyapp.interface_adapter.account.AccountController;
import org.example.studyapp.interface_adapter.generate.GenerateController;
import org.example.studyapp.interface_adapter.generate.GeneratePresenter;
import org.example.studyapp.interface_adapter.storage.StorageController;
import org.example.studyapp.use_case.account.AccountInteractor;
import org.example.studyapp.use_case.generate.GenerateInteractor;
import org.example.studyapp.use_case.storage.StorageInteractor;
import org.example.studyapp.view.StudyWindow;
import org.example.studyapp.interface_adapter.quiz.QuizController;
import org.example.studyapp.use_case.quiz.QuizInteractor;

/** Composition root: concrete infrastructure is selected only here. */
public final class AppBuilder {
    public StudyWindow build() throws Exception {
        Config config = new Config(Path.of(".env"));
        LocalStore localStore = new LocalStore(Path.of(config.get("STUDY_APP_DATA_FILE",
                Path.of(System.getProperty("user.home"), ".study-app", "study-sets.json").toString())));
        CloudStore cloudStore = new CloudStore(config);
        StorageController local = new StorageController(new StorageInteractor(localStore));
        StorageController cloud = new StorageController(new StorageInteractor(cloudStore));
        GenerateController generator = new GenerateController(new GenerateInteractor(new GeminiGenerator(config), new GeneratePresenter()));
        AccountController accounts = new AccountController(new AccountInteractor(cloudStore));
        return new StudyWindow(local, cloud, generator, accounts, new QuizController(new QuizInteractor()), local.load());
    }
}
