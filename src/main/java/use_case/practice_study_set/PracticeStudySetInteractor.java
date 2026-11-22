package use_case.practice_study_set;

import entity.Flashcard;
import entity.StudySet;

import java.util.ArrayList;
import java.util.List;

/**
 * The Interactor for the Practice Study Set use case (Use Case 6). For now it
 * starts a session on the study set's flashcards in their saved order.
 */
public class PracticeStudySetInteractor implements PracticeStudySetInputBoundary {
  private final PracticeStudySetDataAccessInterface dataAccess;
  private final PracticeStudySetOutputBoundary presenter;

  /**
   * Creates the interactor.
   *
   * @param dataAccess storage for study sets
   * @param presenter  receives the success or failure result
   */
  public PracticeStudySetInteractor(PracticeStudySetDataAccessInterface dataAccess,
                                    PracticeStudySetOutputBoundary presenter) {
    this.dataAccess = dataAccess;
    this.presenter = presenter;
  }

  /**
   * Starts a practice session on the study set's flashcards. Fails if the study
   * set cannot be found or has no flashcards.
   *
   * @param inputData the id of the study set to practice
   */
  @Override
  public void execute(PracticeStudySetInputData inputData) {
    final StudySet studySet = dataAccess.getStudySetById(inputData.getStudySetId());
    if (studySet == null) {
      presenter.prepareFailView("Could not find the study set. Please try again.");
      return;
    }
    if (studySet.getCardsList().isEmpty()) {                 // extension 2a
      presenter.prepareFailView("\"" + studySet.getName()
          + "\" has no flashcards yet. Add some in the Study sets tab first.");
      return;
    }

    final List<PracticeStudySetOutputData.Card> cards = new ArrayList<>();
    for (Flashcard flashcard : studySet.getCardsList().getCards()) {
      cards.add(new PracticeStudySetOutputData.Card(flashcard.getQuestion(), flashcard.getAnswer()));
    }
    presenter.prepareSuccessView(new PracticeStudySetOutputData(studySet.getName(), cards));
  }
}
