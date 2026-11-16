package use_case.view_flashcards;

import entity.Flashcard;
import entity.StudySet;

import java.util.ArrayList;
import java.util.List;

/**
 * The Interactor for the View Flashcards use case.
 */
public class ViewFlashcardsInteractor implements ViewFlashcardsInputBoundary {
  private final ViewFlashcardsDataAccessInterface dataAccess;
  private final ViewFlashcardsOutputBoundary presenter;

  /**
   * Creates the interactor.
   *
   * @param dataAccess storage for study sets
   * @param presenter  receives the success or failure result
   */
  public ViewFlashcardsInteractor(ViewFlashcardsDataAccessInterface dataAccess,
                                  ViewFlashcardsOutputBoundary presenter) {
    this.dataAccess = dataAccess;
    this.presenter = presenter;
  }

  /**
   * Looks up the study set and passes its flashcards to the presenter. Fails if
   * the study set cannot be found.
   *
   * @param inputData the id of the study set whose flashcards to show
   */
  @Override
  public void execute(ViewFlashcardsInputData inputData) {
    final StudySet studySet = dataAccess.getStudySetById(inputData.getStudySetId());
    if (studySet == null) {
      presenter.prepareFailView("Could not find the study set. Please open it and try again.");
      return;
    }

    final List<ViewFlashcardsOutputData.Card> cards = new ArrayList<>();
    for (Flashcard flashcard : studySet.getCardsList().getCards()) {
      cards.add(new ViewFlashcardsOutputData.Card(flashcard.getQuestion(), flashcard.getAnswer()));
    }
    presenter.prepareSuccessView(new ViewFlashcardsOutputData(studySet.getName(), cards));
  }
}
