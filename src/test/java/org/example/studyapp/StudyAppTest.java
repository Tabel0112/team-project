package org.example.studyapp;

import org.example.studyapp.data_access.LocalStore;
import org.example.studyapp.entity.StudySet;
import org.example.studyapp.entity.Card;
import org.example.studyapp.data_access.Config;
import org.example.studyapp.data_access.*;
import org.example.studyapp.interface_adapter.generate.GenerateController;
import org.example.studyapp.interface_adapter.generate.GeneratePresenter;
import org.example.studyapp.use_case.generate.GenerateInteractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class StudyAppTest {
    @TempDir Path directory;

    @Test void savesAndRestoresMultipleSetsAndCards() throws Exception {
        StudySet first = new StudySet(); first.title = "Biology"; first.notes = "Cell notes";
        first.cards.add(new Card("What stores DNA?", "The nucleus"));
        StudySet second = new StudySet(); second.title = "Algorithms";
        LocalStore store = new LocalStore(directory.resolve("nested/sets.json"));
        store.save(Arrays.asList(first, second));
        List<StudySet> loaded = store.load();
        assertEquals(2, loaded.size()); assertEquals(first.id, loaded.get(0).id);
        assertEquals("Cell notes", loaded.get(0).notes);
        assertEquals("What stores DNA?", loaded.get(0).cards.get(0).question);
        loaded.get(0).cards.get(0).answer = "Updated answer";
        store.save(loaded);
        assertEquals("Updated answer", store.load().get(0).cards.get(0).answer);
    }
    @Test void loadsLegacyCardsWithRetryStatus() throws Exception {
        Path file = directory.resolve("legacy.json");
        Files.writeString(file, "[{\"id\":\"" + UUID.randomUUID()
                + "\",\"title\":\"Biology\",\"notes\":\"\",\"difficulty\":\"Medium\","
                + "\"cards\":[{\"question\":\"Q\",\"answer\":\"A\",\"missed\":true}]}]");
        LocalStore store = new LocalStore(file);
        List<StudySet> loaded = store.load();
        assertEquals("Q", loaded.get(0).cards.get(0).question);
        assertEquals("A", loaded.get(0).cards.get(0).answer);
        store.save(loaded);
        assertEquals("A", store.load().get(0).cards.get(0).answer);
    }
    @Test void invalidSavePreservesPreviousData() throws Exception {
        LocalStore store = new LocalStore(directory.resolve("sets.json"));
        StudySet set = new StudySet(); store.save(List.of(set));
        set.cards.add(new Card("", ""));
        assertThrows(IllegalArgumentException.class, () -> store.save(List.of(set)));
        assertTrue(store.load().get(0).cards.isEmpty());
    }
    @Test void corruptDataIsReportedAndPreserved() throws Exception {
        Path file = directory.resolve("sets.json"); Files.writeString(file, "{broken");
        assertThrows(java.io.IOException.class, () -> new LocalStore(file).load());
        assertEquals("{broken", Files.readString(file));
    }
    @Test void rejectsMalformedStudySets() {
        StudySet set = new StudySet(); set.id = "not-a-uuid";
        assertThrows(IllegalArgumentException.class, set::validate);
        set.id = UUID.randomUUID().toString(); set.cards.add(null);
        assertThrows(IllegalArgumentException.class, set::validate);
    }
    @Test void readsQuotedEnvValuesAndReportsMissingKeys() throws Exception {
        Path file = directory.resolve(".env");
        Files.writeString(file, "# comment\nSTUDY_TEST_EMPTY=\nexport STUDY_TEST_QUOTED='abc=123'\nSTUDY_TEST_DOUBLE=\"hello\"\n");
        Config config = new Config(file);
        assertEquals("abc=123", config.require("STUDY_TEST_QUOTED"));
        assertEquals("hello", config.require("STUDY_TEST_DOUBLE"));
        assertEquals("default", config.get("STUDY_TEST_EMPTY", "default"));
        assertThrows(IllegalStateException.class, () -> config.require("STUDY_TEST_MISSING"));
    }
    @Test void generationValidatesInputBeforeCallingService() throws Exception {
        GenerateController generator = new GenerateController(new GenerateInteractor(
                new GeminiGenerator(new Config(directory.resolve("missing"))), new GeneratePresenter()));
        assertThrows(IllegalArgumentException.class, () -> generator.generate("Title", "", "Easy", 5));
        assertThrows(IllegalArgumentException.class, () -> generator.generate("Title", "Notes", "Easy", 31));
        assertThrows(IllegalStateException.class, () -> generator.generate("Title", "Notes", "Easy", 5));
    }
}
