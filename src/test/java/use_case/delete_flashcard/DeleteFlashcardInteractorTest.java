package use_case.delete_flashcard;

import data_access.InMemoryStudySetDataAccessObject;
import entity.Flashcard;
import entity.StudySet;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DeleteFlashcardInteractorTest {
    private final InMemoryStudySetDataAccessObject dao = new InMemoryStudySetDataAccessObject();
    private final RecordingPresenter presenter = new RecordingPresenter();
    private final DeleteFlashcardInteractor interactor = new DeleteFlashcardInteractor(dao, presenter);
    private StudySet studySet;

    @Before public void setUp() {
        studySet = new StudySet("set-1", "Biology");
        studySet.getCardsList().addCard(new Flashcard("What is DNA?", "A molecule"));
        studySet.getCardsList().addCard(new Flashcard("What is RNA?", "Another molecule"));
        studySet.getCardsList().addCard(new Flashcard("What is ATP?", "Energy"));
        dao.save("user", studySet);
    }

    @Test public void removesOnlyTheChosenCardAndKeepsOrder() {
        delete(1);
        assertNull(presenter.error);
        assertEquals("set-1", presenter.output.getStudySetId());
        assertEquals("What is RNA?", presenter.output.getDeletedQuestion());
        assertEquals(2, presenter.output.getCardCount());
        assertQuestions("What is DNA?", "What is ATP?");
    }

    @Test public void canDeleteTheLastRemainingCard() {
        delete(0);
        delete(0);
        delete(0);
        assertNull(presenter.error);
        assertEquals(0, presenter.output.getCardCount());
        assertTrue(dao.getStudySetById("set-1").getCardsList().isEmpty());
    }

    @Test public void failsWhenStudySetIsMissing() {
        interactor.execute(new DeleteFlashcardInputData("missing", 0));
        assertNull(presenter.output);
        assertTrue(presenter.error.contains("study set"));
        assertEquals(3, studySet.getCardsList().size());
    }

    @Test public void failsWhenCardIndexIsOutOfRange() {
        delete(3);
        assertNull(presenter.output);
        assertTrue(presenter.error.contains("flashcard"));
        presenter.error = null;
        delete(-1);
        assertTrue(presenter.error.contains("flashcard"));
        assertEquals(3, studySet.getCardsList().size());
    }

    private void delete(int index) {
        interactor.execute(new DeleteFlashcardInputData("set-1", index));
    }

    private void assertQuestions(String... expected) {
        final String[] actual = studySet.getCardsList().getCards().stream()
                .map(Flashcard::getQuestion).toArray(String[]::new);
        assertArrayEquals(expected, actual);
    }

    private static class RecordingPresenter implements DeleteFlashcardOutputBoundary {
        DeleteFlashcardOutputData output;
        String error;

        @Override public void prepareSuccessView(DeleteFlashcardOutputData outputData) {
            output = outputData;
        }

        @Override public void prepareFailView(String errorMessage) {
            error = errorMessage;
        }
    }
}
