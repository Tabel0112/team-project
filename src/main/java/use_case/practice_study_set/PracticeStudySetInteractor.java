package use_case.practice_study_set;

import entity.Flashcard;
import entity.StudySet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The Interactor for the Practice Study Set use case (Use Case 7). It starts a
 * session on the study set's flashcards, in their saved order or shuffled.
 */
public class PracticeStudySetInteractor implements PracticeStudySetInputBoundary {
  private final PracticeStudySetDataAccessInterface dataAccess;
  private final PracticeStudySetOutputBoundary presenter;
  private final Random random;

  /**
   * Creates the interactor.
   *
   * @param dataAccess storage for study sets
   * @param presenter  receives the success or failure result
   */
  public PracticeStudySetInteractor(PracticeStudySetDataAccessInterface dataAccess,
                                    PracticeStudySetOutputBoundary presenter) {
    this(dataAccess, presenter, new Random());
  }

  /**
   * Creates the interactor with a given source of randomness for shuffling, so
   * that tests can make the shuffled order predictable.
   *
   * @param dataAccess storage for study sets
   * @param presenter  receives the success or failure result
   * @param random     used to shuffle the flashcards
   */
  public PracticeStudySetInteractor(PracticeStudySetDataAccessInterface dataAccess,
                                    PracticeStudySetOutputBoundary presenter,
                                    Random random) {
    this.dataAccess = dataAccess;
    this.presenter = presenter;
    this.random = random;
  }

  /**
   * Starts a practice session on the study set's flashcards, shuffling them first
   * if asked to. The study set's own order is never changed. Fails if the study
   * set's flashcards are not loaded or it has none.
   *
   * @param inputData the id of the study set to practice, and whether to shuffle
   */
  @Override
  public void execute(PracticeStudySetInputData inputData) {
    final StudySet studySet = dataAccess.getStudySetById(inputData.getStudySetId());
    if (studySet == null) {
      presenter.prepareFailView("This study set's flashcards aren't loaded yet. "
          + "Open it once in the Study sets tab, then review it here.");
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
    if (inputData.isShuffle()) {                             // extension 2b
      Collections.shuffle(cards, random);
    }
    presenter.prepareSuccessView(new PracticeStudySetOutputData(studySet.getName(), cards));
  }
}
