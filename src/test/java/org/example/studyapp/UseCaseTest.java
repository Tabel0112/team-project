package org.example.studyapp;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.example.studyapp.entity.Card;
import org.example.studyapp.entity.StudySet;
import org.example.studyapp.interface_adapter.generate.GenerateController;
import org.example.studyapp.interface_adapter.generate.GeneratePresenter;
import org.example.studyapp.use_case.account.AccountDataAccessInterface;
import org.example.studyapp.use_case.account.AccountInputData;
import org.example.studyapp.use_case.account.AccountInteractor;
import org.example.studyapp.use_case.generate.GenerateInputData;
import org.example.studyapp.use_case.generate.GenerateInteractor;
import org.example.studyapp.use_case.generate.GenerateOutputData;
import org.example.studyapp.use_case.storage.StorageInteractor;
import org.example.studyapp.use_case.storage.StudySetDataAccessInterface;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UseCaseTest {
    @Test void generationUsesInjectedPortAndPresenter() throws Exception {
        StudySet generated = new StudySet();
        generated.cards.add(new Card("Question", "Answer"));
        GenerateController controller = new GenerateController(new GenerateInteractor((title, notes, level, count) -> {
            assertEquals("Biology", title);
            assertEquals("Cell notes", notes);
            assertEquals("Hard", level);
            assertEquals(1, count);
            return generated;
        }, new GeneratePresenter()));
        GenerateOutputData output = controller.generate("Biology", "Cell notes", "Hard", 1);
        assertSame(generated, output.studySet);
        assertEquals("Generated 1 cards in a new set. Review the answers, then save.", output.message);
    }

    @Test void invalidGenerationNeverCallsExternalPort() {
        GenerateInteractor interactor = new GenerateInteractor((title, notes, level, count) -> {
            fail("Invalid requests must not reach an API.");
            return null;
        }, new GeneratePresenter());
        assertThrows(IllegalArgumentException.class,
                () -> interactor.execute(new GenerateInputData("Title", "Notes", null, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> interactor.execute(new GenerateInputData("Title", "Notes", "Easy", 0)));
        assertThrows(IllegalArgumentException.class,
                () -> interactor.execute(new GenerateInputData("Title", "a".repeat(100001), "Easy", 1)));
    }

    @Test void emptyGenerationDoesNotReachPresenter() {
        GenerateInteractor interactor = new GenerateInteractor((title, notes, level, count) -> new StudySet(),
                set -> { fail("Empty generation must not be presented as success."); return null; });
        assertThrows(IllegalStateException.class,
                () -> interactor.execute(new GenerateInputData("Title", "Notes", "Easy", 1)));
    }

    @Test void storageValidatesBeforeSavingThroughPort() throws Exception {
        AtomicBoolean saved = new AtomicBoolean();
        StudySet set = new StudySet();
        StorageInteractor interactor = new StorageInteractor(new StudySetDataAccessInterface() {
            public List<StudySet> load() { return List.of(set); }
            public void save(List<StudySet> sets) { saved.set(true); }
        });
        assertSame(set, interactor.load().get(0));
        set.title = "";
        assertThrows(IllegalArgumentException.class, () -> interactor.save(List.of(set)));
        assertFalse(saved.get());
        set.title = "Biology";
        interactor.save(List.of(set));
        assertTrue(saved.get());
    }

    @Test void accountValidatesAndDelegatesWithoutConcreteCloudStore() throws Exception {
        AtomicBoolean authenticated = new AtomicBoolean();
        AtomicBoolean signedOut = new AtomicBoolean();
        AccountInteractor interactor = new AccountInteractor(new AccountDataAccessInterface() {
            public void authenticate(String email, String password, boolean signUp) {
                assertEquals("student@example.com", email);
                assertEquals("secret", password);
                assertTrue(signUp);
                authenticated.set(true);
            }
            public void signOut() { signedOut.set(true); }
        });
        assertThrows(IllegalArgumentException.class,
                () -> interactor.authenticate(new AccountInputData("", "secret", false)));
        assertFalse(authenticated.get());
        interactor.authenticate(new AccountInputData("student@example.com", "secret", true));
        assertTrue(authenticated.get());
        interactor.signOut();
        assertTrue(signedOut.get());
    }

}
