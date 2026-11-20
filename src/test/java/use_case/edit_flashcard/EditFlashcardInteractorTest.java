package use_case.edit_flashcard;

import data_access.InMemoryStudySetDataAccessObject;
import entity.Flashcard;
import entity.StudySet;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class EditFlashcardInteractorTest {
    private final InMemoryStudySetDataAccessObject dao = new InMemoryStudySetDataAccessObject();
    private final RecordingPresenter presenter = new RecordingPresenter();
    private final EditFlashcardInteractor interactor = new EditFlashcardInteractor(dao, presenter);
    private StudySet studySet;

    @Before public void setUp() {
        studySet = new StudySet("set-1", "Biology");
        studySet.getCardsList().addCard(new Flashcard("What is DNA?", "A molecule"));
        studySet.getCardsList().addCard(new Flashcard("What is RNA?", "Another molecule"));
        dao.save("user", studySet);
    }

    @Test public void updatesBothSidesAndKeepsPosition() {
        edit(1, "  What does RNA stand for? ", " Ribonucleic acid ");
        assertNull(presenter.error);
        assertEquals("What does RNA stand for?", presenter.output.getQuestion());
        assertEquals("Ribonucleic acid", presenter.output.getAnswer());
        assertEquals("set-1", presenter.output.getStudySetId());
        assertCard(1, "What does RNA stand for?", "Ribonucleic acid");
        assertCard(0, "What is DNA?", "A molecule");
        assertEquals(2, studySet.getCardsList().size());
    }

    @Test public void updatesOnlyOneSide() {
        edit(0, "What is DNA?", "Deoxyribonucleic acid");
        assertNull(presenter.error);
        assertCard(0, "What is DNA?", "Deoxyribonucleic acid");
    }

    @Test public void changesAreVisibleWhenStudySetIsLoadedAgain() {
        edit(0, "Q", "A");
        assertCard(dao.getStudySetById("set-1"), 0, "Q", "A");
    }

    @Test public void rejectsEmptyQuestionWithoutChangingCard() {
        reject(0, "   ", "Deoxyribonucleic acid", "question");
    }

    @Test public void rejectsEmptyAnswerWithoutChangingCard() {
        reject(0, "What is DNA?", "", "answer");
    }

    @Test public void rejectsNullFields() {
        reject(0, null, null, "question");
    }

    @Test public void failsWhenStudySetIsMissing() {
        interactor.execute(new EditFlashcardInputData("missing", 0, "Q", "A"));
        assertNull(presenter.output);
        assertTrue(presenter.error.contains("study set"));
        assertCard(0, "What is DNA?", "A molecule");
    }

    @Test public void failsWhenCardIndexIsOutOfRange() {
        reject(2, "Q", "A", "flashcard");
        reject(-1, "Q", "A", "flashcard");
    }

    private void edit(int index, String question, String answer) {
        interactor.execute(new EditFlashcardInputData("set-1", index, question, answer));
    }

    private void reject(int index, String question, String answer, String expectedInMessage) {
        edit(index, question, answer);
        assertNull(presenter.output);
        assertNotNull(presenter.error);
        assertTrue(presenter.error, presenter.error.contains(expectedInMessage));
        assertCard(0, "What is DNA?", "A molecule");
        assertCard(1, "What is RNA?", "Another molecule");
    }

    private void assertCard(int index, String question, String answer) {
        assertCard(studySet, index, question, answer);
    }

    private static void assertCard(StudySet set, int index, String question, String answer) {
        final Flashcard card = set.getCardsList().getCards().get(index);
        assertEquals(question, card.getQuestion());
        assertEquals(answer, card.getAnswer());
    }

    private static class RecordingPresenter implements EditFlashcardOutputBoundary {
        EditFlashcardOutputData output;
        String error;

        @Override public void prepareSuccessView(EditFlashcardOutputData outputData) {
            output = outputData;
        }

        @Override public void prepareFailView(String errorMessage) {
            error = errorMessage;
        }
    }
}
