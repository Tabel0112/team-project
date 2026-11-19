package use_case.reorder_flashcards;

import data_access.InMemoryStudySetDataAccessObject;
import entity.Flashcard;
import entity.StudySet;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReorderFlashcardsInteractorTest {
    private final InMemoryStudySetDataAccessObject dao = new InMemoryStudySetDataAccessObject();
    private final RecordingPresenter presenter = new RecordingPresenter();
    private final ReorderFlashcardsInteractor interactor =
            new ReorderFlashcardsInteractor(dao, presenter);
    private StudySet studySet;

    @Before public void setUp() {
        studySet = new StudySet("set-1", "Biology");
        studySet.getCardsList().addCard(new Flashcard("A", "1"));
        studySet.getCardsList().addCard(new Flashcard("B", "2"));
        studySet.getCardsList().addCard(new Flashcard("C", "3"));
        dao.save("user", studySet);
    }

    @Test public void movesCardUp() {
        move(2, 1);
        assertNull(presenter.error);
        assertQuestions("A", "C", "B");
        assertEquals(1, presenter.output.getMovedIndex());
        assertEquals("C", presenter.output.getCards().get(1).getQuestion());
        assertEquals("3", presenter.output.getCards().get(1).getAnswer());
    }

    @Test public void movesCardDown() {
        move(0, 1);
        assertNull(presenter.error);
        assertQuestions("B", "A", "C");
    }

    @Test public void movesCardAcrossSeveralPlaces() {
        move(0, 2);
        assertQuestions("B", "C", "A");
        assertEquals(2, presenter.output.getMovedIndex());
    }

    @Test public void newOrderIsKeptWhenStudySetIsLoadedAgain() {
        move(2, 0);
        final String[] questions = dao.getStudySetById("set-1").getCardsList().getCards().stream()
                .map(Flashcard::getQuestion).toArray(String[]::new);
        assertArrayEquals(new String[] {"C", "A", "B"}, questions);
    }

    @Test public void cannotMoveFirstCardUpOrLastCardDown() {
        move(0, -1);
        assertNotNull(presenter.error);
        presenter.error = null;
        move(2, 3);
        assertNotNull(presenter.error);
        assertNull(presenter.output);
        assertQuestions("A", "B", "C");
    }

    @Test public void failsWhenStudySetIsMissing() {
        interactor.execute(new ReorderFlashcardsInputData("missing", 0, 1));
        assertNull(presenter.output);
        assertTrue(presenter.error.contains("study set"));
        assertQuestions("A", "B", "C");
    }

    private void move(int from, int to) {
        interactor.execute(new ReorderFlashcardsInputData("set-1", from, to));
    }

    private void assertQuestions(String... expected) {
        final String[] actual = studySet.getCardsList().getCards().stream()
                .map(Flashcard::getQuestion).toArray(String[]::new);
        assertArrayEquals(expected, actual);
    }

    private static class RecordingPresenter implements ReorderFlashcardsOutputBoundary {
        ReorderFlashcardsOutputData output;
        String error;

        @Override public void prepareSuccessView(ReorderFlashcardsOutputData outputData) {
            output = outputData;
        }

        @Override public void prepareFailView(String errorMessage) {
            error = errorMessage;
        }
    }
}
