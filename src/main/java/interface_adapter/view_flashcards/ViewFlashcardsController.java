package interface_adapter.view_flashcards;

import use_case.view_flashcards.ViewFlashcardsInputBoundary;
import use_case.view_flashcards.ViewFlashcardsInputData;

/**
 * The Controller for the View Flashcards use case.
 */
public class ViewFlashcardsController {
  private final ViewFlashcardsInputBoundary interactor;

  /**
   * Creates the controller.
   *
   * @param interactor the use case to call
   */
  public ViewFlashcardsController(ViewFlashcardsInputBoundary interactor) {
    this.interactor = interactor;
  }

  /**
   * Asks the use case for the flashcards in a study set.
   *
   * @param studySetId the id of the open study set
   */
  public void execute(String studySetId) {
    interactor.execute(new ViewFlashcardsInputData(studySetId));
  }
}
